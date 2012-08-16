/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.log.stats

import com.excilys.ebi.gatling.log.Predef._
import cascading.tuple.TupleEntry
import scala.collection.JavaConversions._
import com.excilys.ebi.gatling.log.util.FieldsNames._
import com.excilys.ebi.gatling.core.util.DateHelper._
import com.excilys.ebi.gatling.core.result.message.RequestStatus
import com.excilys.ebi.gatling.core.result.message.RunRecord

object TupleEntryParser {
	def tupleEntryToGeneralStatsRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new GeneralStatsRecord(get(MIN, map), get(MAX, map), get(SIZE, map), get(MEAN, map), get(MEAN_LATENCY, map), get(MEAN_REQUEST_PER_SEC, map), get(STD_DEV, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	def tupleEntryToResponseTimeDistributionRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new ResponseTimeDistributionRecord(get(RESPONSE_TIME, map), get(SIZE, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	def tupleEntryToRequestPerSecRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new RequestsPerSecRecord(get(EXECUTION_START_BUCKET, map), get(SIZE, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	def tupleEntryToTransactionPerSecRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new TransactionsPerSecRecord(get(EXECUTION_END_BUCKET, map), get(SIZE, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	def tupleEntryToResponseTimePerSecRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new ResponseTimePerSecRecord(get(EXECUTION_START_BUCKET, map), get(RESPONSE_TIME, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	def tupleEntryToLatencyPerSecRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new LatencyPerSecRecord(get(EXECUTION_START_BUCKET, map), get(LATENCY, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	def tupleEntryToRequestAgainstResponseTimeRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new RequestAgainstResponseTimeRecord(get(SIZE, map), get(RESPONSE_TIME, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	def tupleEntryToSessionDeltaRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new SessionDeltaRecord(get(EXECUTION_START_BUCKET, map), get(DELTA, map), getOption(SCENARIO, map))
	}

	def tupleEntryToSessionRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new SessionRecord(get(EXECUTION_START, map), get(SIZE, map), getOption(SCENARIO, map))
	}

	def tupleEntryToRunRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new RunRecord(parseTimestampString(get(DATE, map)), get(ID, map), get(DESCRIPTION, map))
	}

	def tupleEntryToScenarioRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new ScenarioRecord(get(SCENARIO, map))
	}

	private def tupleEntryToMap(tupleEntry: TupleEntry) = {
		tupleEntry.getFields.map(_.toString).zip(tupleEntry.getTuple).toMap
	}

	private def get[A](key: String, map: Map[String, Any]) = map(key).asInstanceOf[A]

	private def getOption[A](key: String, map: Map[String, Any]) = map.get(key).asInstanceOf[Option[A]]

	private def stringToRequestStatus(requestStatusName: Option[String]) = requestStatusName match {
		case None => None
		case Some(name) => Some(RequestStatus.withName(name))
	}
}
