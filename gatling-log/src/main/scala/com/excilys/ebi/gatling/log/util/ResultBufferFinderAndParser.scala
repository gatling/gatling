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
package com.excilys.ebi.gatling.log.util

import cascading.tuple.TupleEntry
import collection.mutable
import com.excilys.ebi.gatling.log.stats.{TupleEntryParser, StatsResults}

object ResultBufferFinderAndParser {
	val GENERAL_STATS = new ResultBufferFinderAndParser(StatsResults.getGeneralStatsBuffer, TupleEntryParser.tupleEntryToGeneralStatsRecord)
	val RESPONSE_TIME_DISTRIBUTION = new ResultBufferFinderAndParser(StatsResults.getResponseTimeDistributionBuffer, TupleEntryParser.tupleEntryToResponseTimeDistributionRecord)
	val REQUESTS_PER_SEC = new ResultBufferFinderAndParser(StatsResults.getRequestsPerSecBuffer, TupleEntryParser.tupleEntryToRequestPerSecRecord)
	val TRANSACTIONS_PER_SEC = new ResultBufferFinderAndParser(StatsResults.getTransactionPerSecBuffer, TupleEntryParser.tupleEntryToTransactionPerSecRecord)
	val RESPONSE_TIME_PER_SEC = new ResultBufferFinderAndParser(StatsResults.getResponseTimePerSecBuffer, TupleEntryParser.tupleEntryToResponseTimePerSecRecord)
	val LATENCY_PER_SEC = new ResultBufferFinderAndParser(StatsResults.getLatencyPerSecBuffer, TupleEntryParser.tupleEntryToLatencyPerSecRecord)
	val REQUEST_AGAINST_RESPONSE_TIME = new ResultBufferFinderAndParser(StatsResults.getRequestAgainstResponseTimeBuffer, TupleEntryParser.tupleEntryToRequestAgainstResponseTimeRecord)
	val SESSION_DELTA = new ResultBufferFinderAndParser(StatsResults.getSessionDeltaBuffer, TupleEntryParser.tupleEntryToSessionDeltaRecord)
	val SESSION = new ResultBufferFinderAndParser(StatsResults.getSessionBuffer, TupleEntryParser.tupleEntryToSessionRecord)
	val RUN_RECORDS = new ResultBufferFinderAndParser(StatsResults.getRunRecordBuffer, TupleEntryParser.tupleEntryToRunRecord)
	val SCENARIO = new ResultBufferFinderAndParser(StatsResults.getScenarioBuffer, TupleEntryParser.tupleEntryToScenarioRecord)
	val REQUEST = new ResultBufferFinderAndParser(StatsResults.getRequestBuffer, TupleEntryParser.tupleEntryToRequestRecord)
}

class ResultBufferFinderAndParser[A](val bufferFinder: (ResultBufferType.ResultBufferType) => mutable.Buffer[A], val parseFunction: (TupleEntry) => A)
