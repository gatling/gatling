/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import io.gatling.core.config.GatlingConfiguration

import scala.concurrent.duration._

object Predef extends CoreDsl {

  implicit var configuration: GatlingConfiguration = _

  type Session = io.gatling.core.session.Session
  type Status = io.gatling.commons.stats.Status
  type Simulation = io.gatling.core.scenario.Simulation
  type Assertion = io.gatling.commons.stats.assertion.Assertion
  type Node = jodd.lagarto.dom.Node

  /**
   * Offers the same implicits conversions as scala.concurrent.duration.DurationInt for java.lang.Integer.
   * @param i the Java's integer that will converted to scala.concurrent.duration.FiniteDuration
   */
  implicit class DurationInteger(val i: Integer) extends AnyVal {

    def nanoseconds = i.toInt.nanoseconds
    def nanos = i.toInt.nanos
    def nanosecond = i.toInt.nanosecond
    def nano = i.toInt.nano

    def microseconds = i.toInt.microseconds
    def micros = i.toInt.micros
    def microsecond = i.toInt.microsecond
    def micro = i.toInt.micro

    def milliseconds = i.toInt.milliseconds
    def millis = i.toInt.millis
    def millisecond = i.toInt.millisecond
    def milli = i.toInt.milli

    def seconds = i.toInt.seconds
    def second = i.toInt.second

    def minutes = i.toInt.minutes
    def minute = i.toInt.minute

    def hours = i.toInt.hours
    def hour = i.toInt.hour

    def days = i.toInt.days
    def day = i.toInt.day
  }

  /**
   * Offers the same implicits conversions as scala.concurrent.duration.DurationInt for java.lang.Long.
   * @param l the Java's Long that will converted to scala.concurrent.duration.FiniteDuration
   */
  implicit class DurationJLong(val l: java.lang.Long) extends AnyVal {

    def nanoseconds = l.toLong.nanoseconds
    def nanos = l.toLong.nanos
    def nanosecond = l.toLong.nanosecond
    def nano = l.toLong.nano

    def microseconds = l.toLong.microseconds
    def micros = l.toLong.micros
    def microsecond = l.toLong.microsecond
    def micro = l.toLong.micro

    def milliseconds = l.toLong.milliseconds
    def millis = l.toLong.millis
    def millisecond = l.toLong.millisecond
    def milli = l.toLong.milli

    def seconds = l.toLong.seconds
    def second = l.toLong.second

    def minutes = l.toLong.minutes
    def minute = l.toLong.minute

    def hours = l.toLong.hours
    def hour = l.toLong.hour

    def days = l.toLong.days
    def day = l.toLong.day
  }
}
