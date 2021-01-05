/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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
   * @param i the Java's integer that will converted to scala.concurrent.duration.FiniteDuration
   */
  implicit class DurationInteger(val i: Integer) extends AnyVal {

    def nanoseconds: FiniteDuration = i.toInt.nanoseconds
    def nanos: FiniteDuration = i.toInt.nanos
    def nanosecond: FiniteDuration = i.toInt.nanosecond
    def nano: FiniteDuration = i.toInt.nano

    def microseconds: FiniteDuration = i.toInt.microseconds
    def micros: FiniteDuration = i.toInt.micros
    def microsecond: FiniteDuration = i.toInt.microsecond
    def micro: FiniteDuration = i.toInt.micro

    def milliseconds: FiniteDuration = i.toInt.milliseconds
    def millis: FiniteDuration = i.toInt.millis
    def millisecond: FiniteDuration = i.toInt.millisecond
    def milli: FiniteDuration = i.toInt.milli

    def seconds: FiniteDuration = i.toInt.seconds
    def second: FiniteDuration = i.toInt.second

    def minutes: FiniteDuration = i.toInt.minutes
    def minute: FiniteDuration = i.toInt.minute

    def hours: FiniteDuration = i.toInt.hours
    def hour: FiniteDuration = i.toInt.hour

    def days: FiniteDuration = i.toInt.days
    def day: FiniteDuration = i.toInt.day
  }

  /**
   * Offers the same implicits conversions as scala.concurrent.duration.DurationInt for java.lang.Long.
   * @param l the Java's Long that will converted to scala.concurrent.duration.FiniteDuration
   */
  implicit class DurationJLong(val l: java.lang.Long) extends AnyVal {

    def nanoseconds: FiniteDuration = l.toLong.nanoseconds
    def nanos: FiniteDuration = l.toLong.nanos
    def nanosecond: FiniteDuration = l.toLong.nanosecond
    def nano: FiniteDuration = l.toLong.nano

    def microseconds: FiniteDuration = l.toLong.microseconds
    def micros: FiniteDuration = l.toLong.micros
    def microsecond: FiniteDuration = l.toLong.microsecond
    def micro: FiniteDuration = l.toLong.micro

    def milliseconds: FiniteDuration = l.toLong.milliseconds
    def millis: FiniteDuration = l.toLong.millis
    def millisecond: FiniteDuration = l.toLong.millisecond
    def milli: FiniteDuration = l.toLong.milli

    def seconds: FiniteDuration = l.toLong.seconds
    def second: FiniteDuration = l.toLong.second

    def minutes: FiniteDuration = l.toLong.minutes
    def minute: FiniteDuration = l.toLong.minute

    def hours: FiniteDuration = l.toLong.hours
    def hour: FiniteDuration = l.toLong.hour

    def days: FiniteDuration = l.toLong.days
    def day: FiniteDuration = l.toLong.day
  }

  implicit def integerToFiniteDuration(i: Integer): FiniteDuration = intToFiniteDuration(i.toInt)

  implicit def intToFiniteDuration(i: Int): FiniteDuration = i.seconds

  implicit def jlongToFiniteDuration(i: java.lang.Long): FiniteDuration = i.toLong.seconds
}
