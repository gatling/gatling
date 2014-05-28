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
package io.gatling.core.assertion

import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.reader.{ DataReader, GeneralStats }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.NumberHelper._
import io.gatling.core.validation.{ Failure, Success, Validation }

class Selector(stats: (DataReader, Option[Status]) => Validation[GeneralStats], name: String) {

  def responseTime = new ResponseTime(reader => stats(reader, None), name)

  def allRequests = new Requests(stats, None, name)

  def failedRequests = new Requests(stats, Some(KO), name)

  def successfulRequests = new Requests(stats, Some(OK), name)

  def requestsPerSec = Metric(reader => stats(reader, None).map(_.meanRequestsPerSec), s"$name: requests per second")
}

object ResponseTime {
  val percentile1 = configuration.charting.indicators.percentile1.toRank
  val percentile2 = configuration.charting.indicators.percentile2.toRank
}

class ResponseTime(responseTime: DataReader => Validation[GeneralStats], name: String) {
  def min = Metric(reader => responseTime(reader).map(_.min), s"$name: min response time")

  def max = Metric(reader => responseTime(reader).map(_.max), s"$name: max response time")

  def mean = Metric(reader => responseTime(reader).map(_.mean), s"$name: mean response time")

  def stdDev = Metric(reader => responseTime(reader).map(_.stdDev), s"$name: standard deviation response time")

  def percentile1 = Metric(reader => responseTime(reader).map(_.percentile1), s"$name: ${ResponseTime.percentile1} percentile response time")

  def percentile2 = Metric(reader => responseTime(reader).map(_.percentile2), s"$name: ${ResponseTime.percentile2} percentile response time")
}

class Requests(requests: (DataReader, Option[Status]) => Validation[GeneralStats], status: Option[Status], name: String) {

  private def message(message: String) = status match {
    case Some(s) => s"$name $message $s"
    case None    => s"$name $message"
  }

  def percent = {
    val value = (reader: DataReader) => for {
      statusStats <- requests(reader, status)
      allStats <- requests(reader, None)
    } yield math.round(statusStats.count.toFloat / allStats.count) / 100

    Metric(value, message("percentage of requests"))
  }

  def count = Metric(reader => requests(reader, status).map(_.count), message("number of requests"))
}

case class Metric[T: Numeric](value: DataReader => Validation[T], name: String, assertions: List[Assertion] = List()) {

  def assert(assertion: (T) => Boolean, message: (String, Boolean) => String) = {
    val newAssertion = new Assertion(reader => value(reader).map(assertion), result => message(name, result))
    copy(assertions = assertions :+ newAssertion)
  }

  def lessThan(threshold: T) = assert(implicitly[Numeric[T]].lt(_, threshold), (name, result) => s"$name is less than $threshold: $result")

  def greaterThan(threshold: T) = assert(implicitly[Numeric[T]].gt(_, threshold), (name, result) => s"$name is greater than $threshold: $result")

  def between(min: T, max: T) = assert(v => implicitly[Numeric[T]].gteq(v, min) && implicitly[Numeric[T]].lteq(v, max), (name, result) => s"$name between $min and $max: $result")

  def is(v: T) = assert(_ == v, (name, result) => s"$name is equal to $v: $result")

  def in(set: Set[T]) = assert(set.contains, (name, result) => s"$name is in $set")
}

object Assertion {
  def assertThat(assertions: Seq[Assertion], dataReader: DataReader): Boolean =
    !assertions
      .map { assertion =>
        assertion(dataReader) match {
          case Success(result) =>
            println(assertion.message(result))
            result

          case Failure(m) =>
            println(m)
            false
        }
      }.contains(false)
}

case class Assertion(assertion: (DataReader) => Validation[Boolean], message: Boolean => String) {
  def apply(reader: DataReader): Validation[Boolean] = assertion(reader)
}
