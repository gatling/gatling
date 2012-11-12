/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import com.excilys.ebi.gatling.core.result.Group
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ KO, OK, RequestStatus }
import com.excilys.ebi.gatling.core.result.reader.{ GeneralStats, DataReader }
import scala.annotation.tailrec
import scala.tools.nsc.io.Path

class AssertionBuilder {
	private def path(reader: DataReader, selector: Path) =
		if (selector.segments.isEmpty) (None, None)
		else {
			val group = selector.segments.foldLeft[Option[Group]](None)((parent, group) => Some(Group(group, parent)))

			reader.groupsAndRequests.find(_ == (group, None)).getOrElse(
				reader.groupsAndRequests.find(_ == (group.flatMap(_.parent), Some(selector.name))).get
			)
		}

	private def generalStats(selector: Path): (DataReader, Option[RequestStatus]) => GeneralStats = (reader, status) => {
		val (group, requestName) = path(reader, selector)
		reader.generalStats(status, requestName, group)
	}

	def global = new Selector((reader, status) => reader.generalStats(status, None, None), "Global")

	def details(selector: Path) = new Selector(generalStats(selector), selector.segments.mkString(" / "))
}

class Selector(stats: (DataReader, Option[RequestStatus]) => GeneralStats, name: String) {
	def responseTime = new ResponseTime(reader => stats(reader, None), name)

	def allRequests = new Requests(stats, None, name)

	def failedRequests = new Requests(stats, Some(KO), name)

	def successfulRequests = new Requests(stats, Some(OK), name)

	def requestsPerSec = new Value(reader => stats(reader, None).meanRequestsPerSec, name + " requests per second")
}

class ResponseTime(responseTime: (DataReader) => GeneralStats, name: String) {
	def min = new Value(reader => responseTime(reader).min, name + " min response time")

	def max = new Value(reader => responseTime(reader).max, name + " max response time")

	def mean = new Value(reader => responseTime(reader).mean, name + " mean response time")

	def stdDev = new Value(reader => responseTime(reader).stdDev, name + " standard deviation response time")

	def perc95 = new Value(reader => responseTime(reader).percentile1, name + " 95th percentile response time")

	def perc99 = new Value(reader => responseTime(reader).percentile2, name + " 99th percentile response time")
}

class Requests(requests: (DataReader, Option[RequestStatus]) => GeneralStats, status: Option[RequestStatus], name: String) {
	private def message(message: String) = status match {
		case Some(status) => name + " " + message + " " + status
		case None => name + " " + message
	}

	def percent = new Value(reader => requests(reader, status).count / requests(reader, None).count, message("percentage of requests"))

	def count = new Value(reader => requests(reader, status).count, message("number of requests"))
}

class Value(val value: (DataReader) => Int, val name: String) {
	def assert(message: (String, Boolean) => String)(assertion: (Int) => Boolean) = new Assertion(this, reader => assertion(value(reader)), result => message(name, result))

	def lessThan(threshold: Int) = assert((name, result) => name + " is less than " + threshold + " : " + result)(value => value <= threshold)

	def greaterThan(threshold: Int) = assert((name, result) => name + " is greater than " + threshold + " : " + result)(value => value >= threshold)

	def between(min: Int, max: Int) = assert((name, result) => name + " between " + min + " and " + max + " : " + result)(value => value >= min && value <= max)

	def is(v: Int) = assert((name, result) => name + " is equal to " + v + " : " + result)(value => value == v)

	def in(seq: Seq[Int]) = assert((name, result) => name + " is in " + seq)(value => seq.contains(value))
}

object Assertion {
	def assertThat(assertions: Seq[Assertion], dataReader: DataReader) = {

		@tailrec
		def assertThat(assertion: Assertion): Boolean = {
			val (result, message) = assertion(dataReader)
			println(message)

			assertion.chainValue match {
				case assertion: Assertion => result && assertThat(assertion)
				case _ => result
			}
		}

		assertions.map(assertThat).foldLeft(true)((accu, result) => accu && result)
	}
}

class Assertion(val chainValue: Value, assertion: (DataReader) => Boolean, message: (Boolean) => String) extends Value(chainValue.value, chainValue.name) {
	def apply(reader: DataReader) = {
		val result = assertion(reader)
		(result, message(result))
	}
}
