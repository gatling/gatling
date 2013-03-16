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
package com.excilys.ebi.gatling.charts.result.reader

import java.io.{ FileInputStream, InputStream }
import java.util.regex.Pattern

import scala.collection.mutable
import scala.io.Source

import com.excilys.ebi.gatling.charts.result.reader.buffers.{ CountBuffer, RangeBuffer }
import com.excilys.ebi.gatling.charts.result.reader.stats.StatsHelper
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogDirectory
import com.excilys.ebi.gatling.core.result.{ Group, IntRangeVsTimePlot, IntVsTimePlot }
import com.excilys.ebi.gatling.core.result.message.{ KO, OK }
import com.excilys.ebi.gatling.core.result.message.{ RequestStatus, RunRecord }
import com.excilys.ebi.gatling.core.result.message.RecordType.{ ACTION, GROUP, RUN, SCENARIO }
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
	val GROUP_RECORD_LENGTH = 6
	val SCENARIO_RECORD_LENGTH = 5
}

class FileDataReader(runUuid: String) extends DataReader(runUuid) with Logging {

	private def multipleFileIterator(streams: Seq[InputStream]): Iterator[String] = streams.map(Source.fromInputStream(_, configuration.simulation.encoding).getLines).reduce((first, second) => first ++ second)

	val inputFiles = simulationLogDirectory(runUuid, create = false).files
		.collect { case file if (file.name.matches(FileDataReader.SIMULATION_FILES_NAME_PATTERN)) => file.jfile }
		.toList

	info(s"Collected $inputFiles from $runUuid")
	require(!inputFiles.isEmpty, "simulation directory doesn't contain any log file.")

	private def doWithInputFiles[T](f: Iterator[String] => T): T = {

		val streams = inputFiles.map(new FileInputStream(_))
		try f(multipleFileIterator(streams))
		finally streams.foreach(_.close)
	}

	private def preProcess(records: Iterator[String]) = {

		info("Pre-process")

		val nonGroupRecordTypes = Set(ACTION, RUN, SCENARIO)
		val (runs, actionsOrScenarios) = records.map(FileDataReader.TABULATION_PATTERN.split).filter(array => nonGroupRecordTypes.contains(array.head)).partition(_.head == RUN)

		def isValidActionRecord(array: Array[String]) = array.head == ACTION && array.length >= FileDataReader.ACTION_RECORD_LENGTH
		def isValidScenarioRecord(array: Array[String]) = array.head == SCENARIO && array.length >= FileDataReader.SCENARIO_RECORD_LENGTH

		val (runStart, runEnd, totalRequestsNumber) = actionsOrScenarios
			.filter { array => isValidActionRecord(array) || isValidScenarioRecord(array) }
			.foldLeft((Long.MaxValue, Long.MinValue, 0L)) {
				(accumulator, strings) =>
					val (min, max, count) = accumulator

					if (count % FileDataReader.LOG_STEP == 0) info(s"First pass, read $count lines")

					strings(0) match {
						case ACTION => (math.min(min, strings(4).toLong), math.max(max, strings(7).toLong), count + 1)
						case SCENARIO => (math.min(min, strings(4).toLong), math.max(max, strings(4).toLong), count + 1)
					}
			}

		val runRecords = mutable.ListBuffer[RunRecord]()

		runs
			.filter(_.length >= FileDataReader.RUN_RECORD_LENGTH)
			.foreach(strings => runRecords += RunRecord(parseTimestampString(strings(1)), strings(2), strings(3).trim))

		info(s"Pre-process done: read $totalRequestsNumber lines")

		(runStart, runEnd, runRecords.head)
	}

	val (runStart, runEnd, runRecord) = doWithInputFiles(preProcess)

	val step = StatsHelper.step(math.floor(runStart / FileDataReader.SEC_MILLISEC_RATIO).toInt, math.ceil(runEnd / FileDataReader.SEC_MILLISEC_RATIO).toInt, configuration.charting.maxPlotsPerSeries) * FileDataReader.SEC_MILLISEC_RATIO
	val bucketFunction = StatsHelper.bucket(_: Int, 0, (runEnd - runStart).toInt, step, step / 2)
	val buckets = StatsHelper.bucketsList(0, (runEnd - runStart).toInt, step)

	private def process(bucketFunction: Int => Int)(records: Iterator[String]): ResultsHolder = {

		info("Process")

		val resultsHolder = new ResultsHolder(runStart, runEnd)

		var count = 0

		records
			.collect { case line if (line.startsWith(ACTION) || line.startsWith(GROUP) || line.startsWith(SCENARIO)) => FileDataReader.TABULATION_PATTERN.split(line) }
			.filter(_.length >= 1)
			.foreach { array =>
				count += 1
				if (count % FileDataReader.LOG_STEP == 0) info(s"First pass, read $count lines")
				array(0) match {
					case ACTION if (array.length >= FileDataReader.ACTION_RECORD_LENGTH) => resultsHolder.addActionRecord(ActionRecord(array, bucketFunction, runStart))
					case GROUP if (array.length >= FileDataReader.GROUP_RECORD_LENGTH) => resultsHolder.addGroupRecord(GroupRecord(array, bucketFunction, runStart))
					case SCENARIO if (array.length >= FileDataReader.SCENARIO_RECORD_LENGTH) => resultsHolder.addScenarioRecord(ScenarioRecord(array, bucketFunction, runStart))
				}
			}

		info(s"Process done: read $count lines")

		resultsHolder
	}

	val resultsHolder = doWithInputFiles(process(bucketFunction))

	def groupsAndRequests: List[(Option[Group], Option[String])] =
		resultsHolder.groupAndRequestsNameBuffer.map.toList.map {
			case ((group, Some(request)), time) => ((group, Some(request)), (time, group.map(_.groups.length + 1).getOrElse(0)))
			case ((Some(group), None), time) => ((Some(group), None), (time, group.groups.length))
			case _ => throw new UnsupportedOperationException
		}.sortBy(_._2).map(_._1)

	def scenarioNames: List[String] = resultsHolder.scenarioNameBuffer
		.map
		.toList
		.sortBy(_._2)
		.map(_._1)

	def numberOfActiveSessionsPerSecond(scenarioName: Option[String]): Seq[IntVsTimePlot] = resultsHolder
		.getSessionDeltaPerSecBuffers(scenarioName)
		.compute(buckets)

	private def countBuffer2IntVsTimePlots(buffer: CountBuffer): Seq[IntVsTimePlot] = buffer
		.map
		.values.toSeq
		.map(plot => plot.copy(value = (plot.value / step * FileDataReader.SEC_MILLISEC_RATIO).toInt))
		.sortBy(_.time)

	def numberOfRequestsPerSecond(status: Option[RequestStatus], requestName: Option[String], group: Option[Group]): Seq[IntVsTimePlot] =
		countBuffer2IntVsTimePlots(resultsHolder.getRequestsPerSecBuffer(requestName, group, status))

	def numberOfTransactionsPerSecond(status: Option[RequestStatus], requestName: Option[String], group: Option[Group]): Seq[IntVsTimePlot] =
		countBuffer2IntVsTimePlots(resultsHolder.getTransactionsPerSecBuffer(requestName, group, status))

	def responseTimeDistribution(slotsNumber: Int, requestName: Option[String], group: Option[Group]): (Seq[IntVsTimePlot], Seq[IntVsTimePlot]) = {

		// get main and max for request/all status
		val requestStats = resultsHolder.getGeneralStatsBuffers(requestName, group, None).compute
		val min = requestStats.min
		val max = requestStats.max

		val size = requestStats.count
		val step = StatsHelper.step(min, max, 100)
		val halfStep = step / 2
		val buckets = StatsHelper.bucketsList(min, max, step)
		val ok = resultsHolder.getGeneralStatsBuffers(requestName, group, Some(OK)).map.values.toSeq
		val ko = resultsHolder.getGeneralStatsBuffers(requestName, group, Some(KO)).map.values.toSeq

		val bucketFunction = StatsHelper.bucket(_: Int, min, max, step, halfStep)

		def process(buffer: Seq[IntVsTimePlot]): List[IntVsTimePlot] = {

			val bucketsWithValues = buffer
				.map(record => (bucketFunction(record.time), record))
				.groupBy(_._1)
				.map {
					case (responseTimeBucket, recordList) =>

						val sizeBucket = recordList.foldLeft(0) {
							(partialSize, record) => partialSize + record._2.value
						}

						(responseTimeBucket, math.round(sizeBucket * 100.0 / size).toInt)
				}
				.toMap

			buckets.map {
				bucket => IntVsTimePlot(bucket, bucketsWithValues.getOrElse(bucket, 0))
			}
		}

		(process(ok), process(ko))
	}

	def generalStats(status: Option[RequestStatus], requestName: Option[String], group: Option[Group]): GeneralStats = resultsHolder
		.getGeneralStatsBuffers(requestName, group, status)
		.compute

	def numberOfRequestInResponseTimeRange(requestName: Option[String], group: Option[Group]): Seq[(String, Int)] = {

		val counts = resultsHolder.getResponseTimeRangeBuffers(requestName, group)
		val lowerBound = configuration.charting.indicators.lowerBound
		val higherBound = configuration.charting.indicators.higherBound

		List((s"t < $lowerBound ms", counts.low),
			(s"$lowerBound ms < t < $higherBound ms", counts.middle),
			(s"t > $higherBound ms", counts.high),
			("failed", counts.ko))
	}

	private def rangeBuffer2IntRangeVsTimePlots(buffer: RangeBuffer): Seq[IntRangeVsTimePlot] = buffer
		.map
		.values
		.toSeq
		.sortBy(_.time)

	def responseTimeGroupByExecutionStartDate(status: RequestStatus, requestName: Option[String], group: Option[Group]): Seq[IntRangeVsTimePlot] =
		rangeBuffer2IntRangeVsTimePlots(resultsHolder.getResponseTimePerSecBuffers(requestName, group, Some(status)))

	def latencyGroupByExecutionStartDate(status: RequestStatus, requestName: Option[String], group: Option[Group]): Seq[IntRangeVsTimePlot] =
		rangeBuffer2IntRangeVsTimePlots(resultsHolder.getLatencyPerSecBuffers(requestName, group, Some(status)))

	def responseTimeAgainstGlobalNumberOfRequestsPerSec(status: RequestStatus, requestName: Option[String], group: Option[Group]): Seq[IntVsTimePlot] = {

		val globalCountsByBucket = resultsHolder.getRequestsPerSecBuffer(None, None, None).map

		resultsHolder
			.getResponseTimePerSecBuffers(requestName, group, Some(status))
			.map
			.toSeq
			.map {
				case (bucket, responseTimes) =>
					val count = globalCountsByBucket(bucket).value
					IntVsTimePlot(math.round(count / step * 1000).toInt, responseTimes.higher)
			}.sortBy(_.time)
	}
}