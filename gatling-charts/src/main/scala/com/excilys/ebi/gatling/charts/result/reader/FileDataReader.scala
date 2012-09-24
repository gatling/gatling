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
package com.excilys.ebi.gatling.charts.result.reader

import java.io.File
import java.util.{ HashMap => JHashMap }
import java.util.regex.Pattern

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.mutable
import scala.io.Source

import com.excilys.ebi.gatling.charts.result.reader.stats.StatsHelper
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogDirectory
import com.excilys.ebi.gatling.core.result.message.RecordType.{ ACTION, RUN }
import com.excilys.ebi.gatling.core.result.message.RequestStatus
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ KO, OK }
import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.core.result.reader.{ DataReader, GeneralStats }
import com.excilys.ebi.gatling.core.util.DateHelper.parseTimestampString
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR

import grizzled.slf4j.Logging

object FileDataReader {
	val LOG_STEP = 100000
	val SEC_MILLISEC_RATIO = 1000.0
	val NO_PLOT_MAGIC_VALUE = -1L
	val TABULATION_PATTERN = Pattern.compile(TABULATION_SEPARATOR)
	val SIMULATION_FILES_NAME_PATTERN = """.*\.log"""
	val ACTION_RECORD_LENGTH = 9
	val RUN_RECORD_LENGTH = 4
}

class FileDataReader(runUuid: String,
					 lowerBound: Int = configuration.charting.indicators.lowerBound,
					 higherBound: Int = configuration.charting.indicators.higherBound,
					 percentile1: Int = configuration.charting.indicators.percentile1,
					 percentile2: Int = configuration.charting.indicators.percentile2)
	extends DataReader(runUuid) with Logging {

	private def multipleFileIterator(files: Seq[File]): Iterator[String] = files.map(file => Source.fromFile(file, configuration.simulation.encoding).getLines()).reduce((first, second) => first ++ second)

	val inputFiles = simulationLogDirectory(runUuid, create = false).files.filter(_.jfile.getName.matches(FileDataReader.SIMULATION_FILES_NAME_PATTERN)).map(_.jfile).toSeq

	if (inputFiles.isEmpty) throw new IllegalArgumentException("simulation directory doesn't contain any log file.")

	private def preProcess(records: Iterator[String]) = {
		val (actions, runs) = records.map(FileDataReader.TABULATION_PATTERN.split(_)).filter(array => array.head == ACTION || array.head == RUN).partition(_.head == ACTION)

		val (runStart, runEnd, totalRequestsNumber) = actions
			.filter(_.length >= FileDataReader.ACTION_RECORD_LENGTH)
			.foldLeft((Long.MaxValue, Long.MinValue, 0L)) {
				(accumulator, strings) =>
					val (min, max, count) = accumulator

					if (count % FileDataReader.LOG_STEP == 0) info("Read " + count + " lines")

					(math.min(min, strings(4).toLong), math.max(max, strings(5).toLong), count + 1)
			}

		val runRecords = mutable.ListBuffer[RunRecord]()
		runs
			.filter(_.length >= FileDataReader.RUN_RECORD_LENGTH)
			.foreach(strings => runRecords += RunRecord(parseTimestampString(strings(1)), strings(2), strings(3).trim))

		info("Read " + totalRequestsNumber + " lines (finished)")

		(runStart, runEnd, runRecords.head)
	}

	val (runStart, runEnd, runRecord) = preProcess(multipleFileIterator(inputFiles))

	val step = StatsHelper.step(math.floor(runStart / FileDataReader.SEC_MILLISEC_RATIO).toLong, math.ceil(runEnd / FileDataReader.SEC_MILLISEC_RATIO).toLong, configuration.charting.maxPlotsPerSeries) * FileDataReader.SEC_MILLISEC_RATIO
	val bucketFunction = StatsHelper.bucket(_: Long, runStart, runEnd, step, step / 2)
	val buckets = StatsHelper.bucketsList(runStart, runEnd, step)

	private def process(records: Iterator[String], bucketFunction: Long => Long): ResultsHolder = {

		val resultsHolder = new ResultsHolder(runStart, runEnd,percentile1,percentile2,lowerBound,higherBound)

		records
			.filter(_.startsWith(ACTION))
			.map(FileDataReader.TABULATION_PATTERN.split(_))
			.filter(_.size >= FileDataReader.ACTION_RECORD_LENGTH)
			.map(ActionRecord(_, bucketFunction))
			.foreach(resultsHolder.add(_))

		resultsHolder
	}

	val resultsHolder = process(multipleFileIterator(inputFiles), bucketFunction)

	def requestNames: Seq[String] = resultsHolder
		.requestNameBuffer
		.map.toList
		.sortBy(_._2)
		.map(_._1)

	def scenarioNames: Seq[String] = resultsHolder.scenarioNameBuffer
		.map
		.toList
		.sortBy(_._2)
		.map(_._1)

	def numberOfActiveSessionsPerSecond(scenarioName: Option[String]): Seq[(Long, Long)] = resultsHolder
		.getSessionDeltaPerSecBuffers(scenarioName)
		.compute(buckets)

	def numberOfRequestsPerSecond(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Seq[(Long, Long)] = resultsHolder
		.getRequestsPerSecBuffer(requestName, status).map
		.toList
		.map {
			case (bucket, count) => (bucket, math.round(count / step * FileDataReader.SEC_MILLISEC_RATIO))
		}
		.sorted

	def numberOfTransactionsPerSecond(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Seq[(Long, Long)] = resultsHolder
		.getTransactionsPerSecBuffer(requestName, status).map
		.toList
		.map {
			case (bucket, count) => (bucket, math.round(count / step * FileDataReader.SEC_MILLISEC_RATIO))
		}
		.sorted

	def responseTimeDistribution(slotsNumber: Int, requestName: Option[String]): (Seq[(Long, Long)], Seq[(Long, Long)]) = {

		// get main and max for request/all status
		val requestStats = resultsHolder.getGeneralStatsBuffers(requestName, None).compute
		val min = requestStats.min
		val max = requestStats.max

		val size = requestStats.count
		val step = StatsHelper.step(min, max, 100)
		val demiStep = step / 2
		val buckets = StatsHelper.bucketsList(min, max, step)
		val ok = resultsHolder.getGeneralStatsBuffers(requestName, Some(OK)).map.toList
		val ko = resultsHolder.getGeneralStatsBuffers(requestName, Some(KO)).map.toList

		val bucketFunction = StatsHelper.bucket(_: Long, min, max, step, demiStep)

		def process(buffer: List[(Long, Long)]) = {

			val bucketsWithValues = buffer
				.map(record => (bucketFunction(record._1), record))
				.groupBy(_._1)
				.map {
					case (responseTimeBucket, recordList) =>

						val sizeBucket = recordList.foldLeft(0L) {
							(partialSize, record) => partialSize + record._2._2
						}

						(responseTimeBucket, math.round(sizeBucket * 100.0 / size))
				}
				.toMap

			buckets.map {
				bucket => (bucket, bucketsWithValues.getOrElse(bucket, 0L))
			}
		}

		(process(ok), process(ko))
	}

	def generalStats(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): GeneralStats = resultsHolder
		.getGeneralStatsBuffers(requestName, status)
		.compute

	def numberOfRequestInResponseTimeRange(requestName: Option[String]): Seq[(String, Long)] = {

		val counts = resultsHolder.getResponseTimeRangeBuffers(requestName)

		List(("t < " + lowerBound + " ms", counts.low),
			(lowerBound + " ms < t < " + higherBound + " ms", counts.middle),
			("t > " + higherBound + " ms", counts.high),
			("failed", counts.ko))
	}

	def responseTimeGroupByExecutionStartDate(status: RequestStatus.RequestStatus, requestName: String): Seq[(Long, (Long, Long))] = resultsHolder
		.getResponseTimePerSecBuffers(Some(requestName), Some(status))
		.map
		.toList
		.sorted

	def latencyGroupByExecutionStartDate(status: RequestStatus.RequestStatus, requestName: String): Seq[(Long, (Long, Long))] = resultsHolder
		.getLatencyPerSecBuffers(Some(requestName), Some(status))
		.map
		.toList
		.sorted

	def responseTimeAgainstGlobalNumberOfRequestsPerSec(status: RequestStatus.RequestStatus, requestName: String): Seq[(Long, Long)] = {

		val globalCountsByBucket: JHashMap[Long, Long] = resultsHolder.getRequestsPerSecBuffer(None, None).map

		resultsHolder
			.getResponseTimePerSecBuffers(Some(requestName), Some(status))
			.map
			.toList
			.map {
				case (bucket, responseTimes) =>
					val (_, max) = responseTimes
					val count = globalCountsByBucket.get(bucket)
					(math.round(count / step * 1000), max)
			}.sorted
	}
}