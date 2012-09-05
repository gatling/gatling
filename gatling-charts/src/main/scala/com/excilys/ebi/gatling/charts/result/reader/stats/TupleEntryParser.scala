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
package com.excilys.ebi.gatling.charts.result.reader.stats

import scala.collection.JavaConversions.iterableAsScalaIterable

import com.excilys.ebi.gatling.charts.result.reader.Predef.symbolToString
import com.excilys.ebi.gatling.charts.result.reader.util.FieldsNames._
import com.excilys.ebi.gatling.core.result.message.{ RequestStatus, RunRecord }
import com.excilys.ebi.gatling.core.util.DateHelper.parseTimestampString

import cascading.tuple.TupleEntry

object TupleEntryParser {
	implicit def tupleEntryToGeneralStatsRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new GeneralStatsRecord(get(MIN, map), get(MAX, map), get(SIZE, map), get(MEAN, map), get(MEAN_LATENCY, map), get(MEAN_REQUEST_PER_SEC, map), get(STD_DEV, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	implicit def tupleEntryToResponseTimeDistributionRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new ResponseTimeDistributionRecord(get(RESPONSE_TIME, map), get(SIZE, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	implicit def tupleEntryToRequestPerSecRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new RequestsPerSecRecord(get(EXECUTION_START_BUCKET, map), get(SIZE, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	implicit def tupleEntryToTransactionPerSecRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new TransactionsPerSecRecord(get(EXECUTION_END_BUCKET, map), get(SIZE, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	implicit def tupleEntryToResponseTimePerSecRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new ResponseTimePerSecRecord(get(EXECUTION_START_BUCKET, map), get(RESPONSE_TIME_MIN, map), get(RESPONSE_TIME_MAX, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	implicit def tupleEntryToLatencyPerSecRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new LatencyPerSecRecord(get(EXECUTION_START_BUCKET, map), get(LATENCY_MIN, map), get(LATENCY_MAX, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	implicit def tupleEntryToRequestAgainstResponseTimeRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new RequestAgainstResponseTimeRecord(get(SIZE, map), get(RESPONSE_TIME_MAX, map), stringToRequestStatus(getOption(STATUS, map)), getOption(REQUEST, map))
	}

	implicit def tupleEntryToSessionDeltaRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new SessionDeltaRecord(get(EXECUTION_START_BUCKET, map), get(NB_SESSION_START, map), get(NB_SESSION_END, map), getOption(SCENARIO, map))
	}

	implicit def tupleEntryToSessionRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new SessionRecord(get(EXECUTION_START, map), get(SIZE, map), getOption(SCENARIO, map))
	}

	implicit def tupleEntryToRunRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new RunRecord(parseTimestampString(get(DATE, map)), get(ID, map), get(DESCRIPTION, map), get(SIMULATION, map))
	}

	implicit def tupleEntryToScenarioRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new ScenarioRecord(get(SCENARIO, map), get(EXECUTION_START, map))
	}

	implicit def tupleEntryToRequestRecord(tupleEntry: TupleEntry) = {
		val map = tupleEntryToMap(tupleEntry)
		new RequestRecord(get(REQUEST, map), get(EXECUTION_START, map))
	}

	private def tupleEntryToMap(tupleEntry: TupleEntry): Map[String, Any] = {
		tupleEntry.getFields.map(_.toString).zip(tupleEntry.getTuple).toMap
	}

	private def get[A](key: String, map: Map[String, Any]) = map(key).asInstanceOf[A]

	private def getOption[A](key: String, map: Map[String, Any]) = map.get(key).asInstanceOf[Option[A]]

	private def stringToRequestStatus(requestStatusName: Option[String]) = requestStatusName match {
		case None => None
		case Some(name) => Some(RequestStatus.withName(name))
	}
}
