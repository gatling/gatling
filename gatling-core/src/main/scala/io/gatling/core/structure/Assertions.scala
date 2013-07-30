/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import io.gatling.core.result.{ Group, StatsPath, GroupStatsPath, RequestStatsPath }
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.result.reader.{ DataReader, GeneralStats }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.NumberHelper

class AssertionBuilder {

	def global = new Selector((reader, status) => reader.requestGeneralStats(None, None, status), "Global")

	def details(selector: Path) = {

		def path(reader: DataReader, selector: Path): Option[StatsPath] =
			if (selector.segments.isEmpty)
				None
			else {
				val selectedPath = selector.segments
				reader.statsPaths.find { statsPath =>
					val path = statsPath match {
						case RequestStatsPath(request, group) => group.map(_.hierarchy).getOrElse(Nil) :: List(request)
						case GroupStatsPath(group) => group.hierarchy
					}
					path == selectedPath
				}
			}

		def generalStats(selector: Path): (DataReader, Option[Status]) => GeneralStats = {
			(reader, status) =>
				path(reader, selector) match {
					case Some(RequestStatsPath(request, group)) => reader.requestGeneralStats(Some(request), group, status)
					case Some(GroupStatsPath(group)) => reader.requestGeneralStats(None, Some(group), status)
					case None => reader.requestGeneralStats(None, None, status)
				}
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

	def is(v: Int) = assert(_ == v, (name, result) => s"$name is equal to $v : $result")

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
