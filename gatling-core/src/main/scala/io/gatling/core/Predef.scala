/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.core

import scala.concurrent.duration._

import io.gatling.core.config.GatlingConfiguration

object Predef extends CoreDsl {
  private[gatling] var _configuration: GatlingConfiguration = _
  implicit def configuration: GatlingConfiguration = {
    if (_configuration == null) {
      throw new IllegalStateException("Simulations can't be instantiated directly but only by Gatling.")
    }
    _configuration
  }

  type Session = io.gatling.core.session.Session
  type Status = io.gatling.commons.stats.Status
  type Simulation = io.gatling.core.scenario.Simulation
  type Assertion = io.gatling.commons.stats.assertion.Assertion
  type Node = _root_.jodd.lagarto.dom.Node

  /**
   * Offers the same implicits conversions as scala.concurrent.duration.DurationInt for java.lang.Integer.
   * @param i
   *   the Java's integer that will converted to scala.concurrent.duration.FiniteDuration
   */
  implicit class DurationInteger(val i: Integer) extends AnyVal {
    def nanoseconds: FiniteDuration = i.intValue.nanoseconds
    def nanos: FiniteDuration = i.intValue.nanos
    def nanosecond: FiniteDuration = i.intValue.nanosecond
    def nano: FiniteDuration = i.intValue.nano

    def microseconds: FiniteDuration = i.intValue.microseconds
    def micros: FiniteDuration = i.intValue.micros
    def microsecond: FiniteDuration = i.intValue.microsecond
    def micro: FiniteDuration = i.intValue.micro

    def milliseconds: FiniteDuration = i.intValue.milliseconds
    def millis: FiniteDuration = i.intValue.millis
    def millisecond: FiniteDuration = i.intValue.millisecond
    def milli: FiniteDuration = i.intValue.milli

    def seconds: FiniteDuration = i.intValue.seconds
    def second: FiniteDuration = i.intValue.second

    def minutes: FiniteDuration = i.intValue.minutes
    def minute: FiniteDuration = i.intValue.minute

    def hours: FiniteDuration = i.intValue.hours
    def hour: FiniteDuration = i.intValue.hour

    def days: FiniteDuration = i.intValue.days
    def day: FiniteDuration = i.intValue.day
  }

  /**
   * Offers the same implicits conversions as scala.concurrent.duration.DurationInt for java.lang.Long.
   * @param l
   *   the Java's Long that will converted to scala.concurrent.duration.FiniteDuration
   */
  implicit class DurationJLong(val l: java.lang.Long) extends AnyVal {
    def nanoseconds: FiniteDuration = l.longValue.nanoseconds
    def nanos: FiniteDuration = l.longValue.nanos
    def nanosecond: FiniteDuration = l.longValue.nanosecond
    def nano: FiniteDuration = l.longValue.nano

    def microseconds: FiniteDuration = l.longValue.microseconds
    def micros: FiniteDuration = l.longValue.micros
    def microsecond: FiniteDuration = l.longValue.microsecond
    def micro: FiniteDuration = l.longValue.micro

    def milliseconds: FiniteDuration = l.longValue.milliseconds
    def millis: FiniteDuration = l.longValue.millis
    def millisecond: FiniteDuration = l.longValue.millisecond
    def milli: FiniteDuration = l.longValue.milli

    def seconds: FiniteDuration = l.longValue.seconds
    def second: FiniteDuration = l.longValue.second

    def minutes: FiniteDuration = l.longValue.minutes
    def minute: FiniteDuration = l.longValue.minute

    def hours: FiniteDuration = l.longValue.hours
    def hour: FiniteDuration = l.longValue.hour

    def days: FiniteDuration = l.longValue.days
    def day: FiniteDuration = l.longValue.day
  }

  implicit def integerToFiniteDuration(i: Integer): FiniteDuration = intToFiniteDuration(i.intValue)

  implicit def intToFiniteDuration(i: Int): FiniteDuration = i.seconds

  implicit def jlongToFiniteDuration(i: java.lang.Long): FiniteDuration = i.longValue.seconds
}
