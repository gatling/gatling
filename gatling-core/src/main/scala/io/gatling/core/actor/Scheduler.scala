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

import java.util.concurrent.{ ScheduledExecutorService, TimeUnit }

import scala.concurrent.duration.FiniteDuration

final class Scheduler(scheduler: ScheduledExecutorService) extends AutoCloseable {

  def scheduleOnce(duration: FiniteDuration)(task: => Unit): Cancellable = {
    val future = scheduler.schedule(
      (() => task): Runnable,
      duration.toMillis,
      TimeUnit.MILLISECONDS
    )

    () => future.cancel(true)
  }

  def scheduleAtFixedRate(period: FiniteDuration)(task: => Unit): Cancellable =
    scheduleAtFixedRate(period, period)(task)

  def scheduleAtFixedRate(initialDelay: FiniteDuration, period: FiniteDuration)(task: => Unit): Cancellable = {
    val future = scheduler.scheduleAtFixedRate(() => task, initialDelay.toMillis, period.toMillis, TimeUnit.MILLISECONDS)
    () => future.cancel(true)
  }

  // [ee]
  def scheduleWithFixedDelay(initialDelay: FiniteDuration, period: FiniteDuration)(
      task: => Unit
  )(implicit ec: scala.concurrent.ExecutionContext): Cancellable = {
    val future = scheduler.scheduleWithFixedDelay(() => ec.execute(() => task), initialDelay.toMillis, period.toMillis, TimeUnit.MILLISECONDS)
    () => future.cancel(true)
  }
  // [ee]

  override def close(): Unit = scheduler.shutdown()
}
