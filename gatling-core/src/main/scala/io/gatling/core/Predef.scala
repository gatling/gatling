/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core

import scala.concurrent.duration.{ DurationInt, FiniteDuration }
import scala.reflect.ClassTag
import scala.reflect.io.Path

import io.gatling.core.assertion.AssertionSupport
import io.gatling.core.check.CheckSupport
import io.gatling.core.controller.inject.InjectionSupport
import io.gatling.core.controller.throttle.ThrottlingSupport
import io.gatling.core.feeder.FeederSupport
import io.gatling.core.pause.PauseSupport
import io.gatling.core.session.{ Expression, ExpressionWrapper }
import io.gatling.core.session.el.EL
import io.gatling.core.structure.{ ScenarioBuilder, StructureSupport }
import io.gatling.core.validation.{ SuccessWrapper, Validation }

object Predef extends StructureSupport with PauseSupport with CheckSupport with FeederSupport with InjectionSupport with ThrottlingSupport with AssertionSupport {

  type Session = io.gatling.core.session.Session
  type Status = io.gatling.core.result.message.Status
  type Simulation = io.gatling.core.scenario.Simulation
  type Assertion = io.gatling.core.assertion.Assertion

  implicit def stringToExpression[T: ClassTag](string: String): Expression[T] = string.el
  implicit def value2Success[T](value: T): Validation[T] = value.success
  implicit def value2Expression[T](value: T): Expression[T] = value.expression

  def scenario(scenarioName: String): ScenarioBuilder = ScenarioBuilder(scenarioName)

  def WhiteList(patterns: String*) = io.gatling.core.filter.WhiteList(patterns.toList)
  def BlackList(patterns: String*) = io.gatling.core.filter.BlackList(patterns.toList)

  implicit def string2path(string: String) = Path.string2path(string)

  def flattenMapIntoAttributes(map: Expression[Map[String, Any]]): Expression[Session] =
    session => map(session).map(resolvedMap => session.setAll(resolvedMap))

  /**********************************/
  /** Duration implicit conversions */
  /**********************************/

  implicit def intToFiniteDuration(i: Int): FiniteDuration = i.seconds
  implicit def integerToFiniteDuration(i: Integer): FiniteDuration = intToFiniteDuration(i.toInt)

  /**
   * Offers the same implicits conversions as [[scala.concurrent.duration.DurationInt]] for Java's Integer.
   * @param i the Java's integer that will converted to [[scala.concurrent.duration.FiniteDuration]]
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
}
