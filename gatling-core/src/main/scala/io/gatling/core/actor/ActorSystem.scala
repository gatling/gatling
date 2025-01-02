/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.actor

import java.util.concurrent.{ ConcurrentLinkedDeque, Executors, TimeoutException }
import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.{ ExecutionContext, Promise }
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

import com.typesafe.scalalogging.StrictLogging
import io.netty.util.internal.PlatformDependent
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue

final class ActorSystem extends AutoCloseable with StrictLogging {

  private val closed = new AtomicBoolean()

  private val executor = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors))
  def executionContext: ExecutionContext = executor
  val scheduler: Scheduler = new Scheduler(Executors.newSingleThreadScheduledExecutor())
  private val onTerminationTasks = new ConcurrentLinkedDeque[() => Unit]

  def actorOf[Message](actor: Actor[Message]): ActorRef[Message] = {
    require(!closed.get(), "Can't spawn an actor with a closed ActorSystem")
    new AtomicRunnableActorRef[Message](actor, this)
  }

  def registerOnTermination[Message](code: => Message): Unit = onTerminationTasks.add(() => code)
  override def close(): Unit =
    if (closed.compareAndSet(false, true)) {
      onTerminationTasks.descendingIterator().forEachRemaining(_.apply())
      executor.shutdown()
      scheduler.close()
    }
}

private object AtomicRunnableActorRef {
  private val MailboxDrainLimit: Int = 20
}

private final class AtomicRunnableActorRef[Message](actor: Actor[Message], system: ActorSystem) extends ActorRef[Message] with Runnable with StrictLogging {

  private val on = new AtomicBoolean
  private val die = new AtomicBoolean
  private val mbox: MessagePassingQueue[Message] = PlatformDependent.newMpscQueue[Message]().asInstanceOf[MessagePassingQueue[Message]]
  private var behavior: Behavior[Message] = {
    actor.initRefs(schedulerRef = system.scheduler, selfRef = this)
    actor.init()
  }

  override def !(msg: Message): Unit = {
    // Enqueue the message onto the mailbox and try to schedule for execution
    mbox.offer(msg)
    async()
  }

  override def replyPromise[Reply](timeout: FiniteDuration): Promise[Reply] = {
    val result = Promise[Reply]()
    val timeoutCancellable = system.scheduler.scheduleOnce(timeout) {
      val timeoutTriggered = result.tryFailure(new TimeoutException())
      if (timeoutTriggered) {
        die.set(true)
        async()
      }
    }
    result.future.onComplete { _ =>
      timeoutCancellable.cancel()
    }(ExecutionContext.parasitic)

    result
  }

  override def name: String = actor.name

  private def async(): Unit =
    if ((!mbox.isEmpty || die.get()) && on.compareAndSet(false, true)) {
      // If there's something to process, and we're not already scheduled
      try {
        system.executionContext.execute(this)
      } catch {
        case NonFatal(t) =>
          logger.error(s"Actor ${actor.name} crashed", t)
          // Schedule to run on the Executor and back out on failure
          on.set(false)
          throw t
      }
    }

  override def run(): Unit =
    try {
      if (on.get()) {
        if (die.compareAndSet(true, false)) {
          behavior = actor.die(behavior)
        } else {
          mbox.drain(
            (m: Message) =>
              try {
                // this is safe because we're guarded by `scheduled`'s memory barrier
                behavior = behavior(m)(behavior)
              } catch {
                case NonFatal(e) =>
                  logger.error(s"Actor ${actor.name} crashed when processing message '$m'", e)
              },
            AtomicRunnableActorRef.MailboxDrainLimit
          )
        }
      }
    } finally {
      // Switch ourselves off, and then see if we should be rescheduled for execution
      on.set(false)
      async()
    }
}
