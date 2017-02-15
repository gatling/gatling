/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
package io.gatling.commons.util

import java.lang.System.{ currentTimeMillis, nanoTime }

import scala.concurrent.duration._

object Clock {
  val ZeroMs = 0 millisecond
}

trait Clock {
  def computeTimeMillisFromNanos(nanos: Long): Long
  def nowMillis: Long
  def unpreciseNowMillis: Long
  def nowSeconds: Long
}

class DefaultClock extends Clock {

  private val currentTimeMillisReference = currentTimeMillis
  private val nanoTimeReference = nanoTime

  override def computeTimeMillisFromNanos(nanos: Long): Long = (nanos - nanoTimeReference) / 1000000 + currentTimeMillisReference
  override def nowMillis: Long = computeTimeMillisFromNanos(nanoTime)
  override def unpreciseNowMillis: Long = currentTimeMillis
  override def nowSeconds: Long = computeTimeMillisFromNanos(nanoTime) / 1000
}

object ClockSingleton extends Clock {

  private val _clock = new DefaultClock

  def loadClockSingleton(): Unit = {}

  override def computeTimeMillisFromNanos(nanos: Long) = _clock.computeTimeMillisFromNanos(nanos)
  override def nowMillis = _clock.nowMillis
  override def unpreciseNowMillis = _clock.unpreciseNowMillis
  override def nowSeconds = _clock.nowSeconds
}
