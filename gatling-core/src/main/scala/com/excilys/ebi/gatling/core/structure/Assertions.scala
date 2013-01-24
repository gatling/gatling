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
package com.excilys.ebi.gatling.core.structure

import scala.tools.nsc.io.Path

import com.excilys.ebi.gatling.core.result.Group
import com.excilys.ebi.gatling.core.result.message.{ KO, OK, RequestStatus }
import com.excilys.ebi.gatling.core.result.reader.{ DataReader, GeneralStats }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.util.NumberHelper

class AssertionBuilder {

	def global = new Selector((reader, status) => reader.generalStats(status, None, None), "Global")

	def details(selector: Path) = {

		type RequestPath = (Option[Group], Option[String])

		def path(reader: DataReader, selector: Path): RequestPath =
			if (selector.segments.isEmpty) (None, None)
			else {
				val criteria = selector.segments.foldLeft[Option[Group]](None)((parent, group) => Some(Group(group, parent)))
				def matchesGroup(requestPath: RequestPath) = requestPath == (criteria, None)
				def matchesRequest(requestPath: RequestPath) = requestPath == (criteria.flatMap(_.parent), Some(selector.name))

				reader.groupsAndRequests.find(matchesGroup)
					.getOrElse(reader.groupsAndRequests.find(matchesRequest)
						.getOrElse(throw new IllegalArgumentException("Path " + selector + " does not exist")))
			}

		def generalStats(selector: Path): (DataReader, Option[RequestStatus]) => GeneralStats = {
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

	def requestsPerSec = Metric(reader => stats(reader, None).meanRequestsPerSec, name + " requests per second")
}

object ResponseTime {
	val PERCENTILE1 = NumberHelper.formatNumberWithSuffix(configuration.charting.indicators.percentile1)
	val PERCENTILE2 = NumberHelper.formatNumberWithSuffix(configuration.charting.indicators.percentile2)
}

class ResponseTime(responseTime: DataReader => GeneralStats, name: String) {
	def min = Metric(reader => responseTime(reader).min, name + " min response time")

	def max = Metric(reader => responseTime(reader).max, name + " max response time")

	def mean = Metric(reader => responseTime(reader).mean, name + " mean response time")

	def stdDev = Metric(reader => responseTime(reader).stdDev, name + " standard deviation response time")

	def percentile1 = Metric(reader => responseTime(reader).percentile1, name + " " + ResponseTime.PERCENTILE1 + " percentile response time")

	def percentile2 = Metric(reader => responseTime(reader).percentile2, name + " " + ResponseTime.PERCENTILE2 + " percentile response time")
}

class Requests(requests: GeneralStatsByStatus, status: Option[RequestStatus], name: String) {

	private def message(message: String) = status match {
		case Some(status) => name + " " + message + " " + status
		case None => name + " " + message
	}

	def percent = Metric(reader => math.round((requests(reader, status).count.toFloat / requests(reader, None).count) * 100), message("percentage of requests"))

	def count = Metric(reader => requests(reader, status).count, message("number of requests"))
}

case class Metric(value: DataReader => Int, name: String, assertions: List[Assertion] = List()) {
	def assert(assertion: (Int) => Boolean, message: (String, Boolean) => String) = copy(assertions = assertions :+ new Assertion(reader => assertion(value(reader)), result => message(name, result)))

	def lessThan(threshold: Int) = assert(_ < threshold, (name, result) => name + " is less than " + threshold + " : " + result)

	def greaterThan(threshold: Int) = assert(_ > threshold, (name, result) => name + " is greater than " + threshold + " : " + result)

	def between(min: Int, max: Int) = assert(value => value >= min && value <= max, (name, result) => name + " between " + min + " and " + max + " : " + result)

	def is(v: Int) = assert( _ == v, (name, result) => name + " is equal to " + v + " : " + result)

	def in(set: Set[Int]) = assert(set.contains, (name, result) => name + " is in " + set)
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
