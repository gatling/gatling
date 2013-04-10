/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.core.structure

import scala.tools.nsc.io.Path

import io.gatling.core.result.Group
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.reader.{ DataReader, GeneralStats }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.NumberHelper

class AssertionBuilder {

	def global = new Selector((reader, status) => reader.generalStats(status, None, None), "Global")

	def details(selector: Path) = {

		type RequestPath = (Option[Group], Option[String])

		def path(reader: DataReader, selector: Path): RequestPath =
			if (selector.segments.isEmpty) (None, None)
			else {
//				val criteria = selector.segments.foldLeft[Option[Group]](None)((parent, group) => Some(Group(group, parent)))
//				def matchesGroup(requestPath: RequestPath) = requestPath == (criteria, None)
//				def matchesRequest(requestPath: RequestPath) = requestPath == (criteria.flatMap(_.parent), Some(selector.name))
//
//				reader.groupsAndRequests.find(matchesGroup)
//					.getOrElse(reader.groupsAndRequests.find(matchesRequest)
//						.getOrElse(throw new IllegalArgumentException(s"Path $selector does not exist")))
				// FIXME
				(None, None)
			}

		def generalStats(selector: Path): (DataReader, Option[Status]) => GeneralStats = {
			(reader, status) =>
				val (group, requestName) = path(reader, selector)
				reader.generalStats(status, requestName, group)
		}

		new Selector(generalStats(selector), selector.segments.mkString(" / "))
	}
}

class Selector(stats: GeneralStatsByStatus, name: String) {
	def responseTime = new ResponseTime(reader => stats(reader, None), name)

	def allRequests = new Requests(stats, None, name)

	def failedRequests = new Requests(stats, Some(KO), name)

	def successfulRequests = new Requests(stats, Some(OK), name)

	def requestsPerSec = Metric(reader => stats(reader, None).meanRequestsPerSec, s"$name : requests per second")
}

object ResponseTime {
	val percentile1 = NumberHelper.formatNumberWithSuffix(configuration.charting.indicators.percentile1)
	val percentile2 = NumberHelper.formatNumberWithSuffix(configuration.charting.indicators.percentile2)
}

class ResponseTime(responseTime: DataReader => GeneralStats, name: String) {
	def min = Metric(reader => responseTime(reader).min, s"$name : min response time")

	def max = Metric(reader => responseTime(reader).max, s"$name : max response time")

	def mean = Metric(reader => responseTime(reader).mean, s"$name : mean response time")

	def stdDev = Metric(reader => responseTime(reader).stdDev, s"$name : standard deviation response time")

	def percentile1 = Metric(reader => responseTime(reader).percentile1, s"$name : ${ResponseTime.percentile1} percentile response time")

	def percentile2 = Metric(reader => responseTime(reader).percentile2, s"$name : ${ResponseTime.percentile2} percentile response time")
}

class Requests(requests: GeneralStatsByStatus, status: Option[Status], name: String) {

	private def message(message: String) = status match {
		case Some(status) => s"$name $message $status"
		case None => s"$name $message"
	}

	def percent = Metric(reader => math.round((requests(reader, status).count.toFloat / requests(reader, None).count) * 100), message("percentage of requests"))

	def count = Metric(reader => requests(reader, status).count, message("number of requests"))
}

case class Metric(value: DataReader => Int, name: String, assertions: List[Assertion] = List()) {
	def assert(assertion: (Int) => Boolean, message: (String, Boolean) => String) = copy(assertions = assertions :+ new Assertion(reader => assertion(value(reader)), result => message(name, result)))

	def lessThan(threshold: Int) = assert(_ < threshold, (name, result) => s"$name is less than $threshold : $result")

	def greaterThan(threshold: Int) = assert(_ > threshold, (name, result) => s"$name is greater than $threshold : $result")

	def between(min: Int, max: Int) = assert(value => value >= min && value <= max, (name, result) => s"$name between $min and $max : $result")

	def is(v: Int) = assert( _ == v, (name, result) => s"$name is equal to $v : $result")

	def in(set: Set[Int]) = assert(set.contains, (name, result) => s"$name is in $set")
}

object Assertion {
	def assertThat(assertions: Seq[Assertion], dataReader: DataReader) =
		assertions.foldLeft(true)((result, assertion) => assertion(dataReader) && result)
}

class Assertion(assertion: (DataReader) => Boolean, message: (Boolean) => String) {
	def apply(reader: DataReader) = {
		val result = assertion(reader)
		println(message(result))
		result
	}
}
