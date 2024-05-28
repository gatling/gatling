/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import java.util.concurrent.{ ArrayBlockingQueue, TimeUnit }

import scala.concurrent.Promise
import scala.concurrent.duration.{ DurationInt, FiniteDuration }
import scala.reflect.ClassTag

import io.gatling.BaseSpec
import io.gatling.core.EmptySession

import org.scalatest.matchers.should.Matchers._

final class MockActorRef[T](override val name: String) extends ActorRef[T] {

  private val queue = new ArrayBlockingQueue[T](100)

  override def !(msg: T): Unit = queue.add(msg)
  override def replyPromise[R](timeout: FiniteDuration): Promise[R] = throw new UnsupportedOperationException

  def expectNoMsg(): Unit = queue.isEmpty shouldBe true

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def expectMsgType[M <: T: ClassTag](timeout: FiniteDuration = 1.seconds): M = {
    val msg = queue.poll(timeout.toMillis, TimeUnit.MILLISECONDS)
    msg shouldNot be(null)
    msg shouldBe a[M]
    msg.asInstanceOf[M]
  }
}

abstract class ActorSpec extends BaseSpec with EmptySession {

  protected val actorSystem = new ActorSystem

  override protected def afterAll(): Unit = actorSystem.close()

  protected def mockActorRef[T](name: String) = new MockActorRef[T](name)
}
