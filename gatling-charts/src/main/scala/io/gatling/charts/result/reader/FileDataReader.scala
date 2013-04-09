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
package io.gatling.charts.result.reader

import java.io.{ FileInputStream, InputStream }

import scala.collection.mutable
import scala.io.Source

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.charts.result.reader.buffers.{ CountBuffer, RangeBuffer }
import io.gatling.charts.result.reader.stats.StatsHelper
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles.simulationLogDirectory
import io.gatling.core.result.{ Group, IntRangeVsTimePlot, IntVsTimePlot }
import io.gatling.core.result.message.{ ActionRecordType, GroupRecordType, KO, OK, RequestStatus, RunRecord, RunRecordType, ScenarioRecordType }
import io.gatling.core.result.reader.{ DataReader, GeneralStats }
import io.gatling.core.util.DateHelper.parseTimestampString
import io.gatling.core.util.FileHelper.tabulationSeparator

object FileDataReader {
	val logStep = 100000
	val secMillisecRatio = 1000.0
	val noPlotMagicValue = -1L
	val simulationFilesNamePattern = """.*\.log"""
}

class FileDataReader(runUuid: String) extends DataReader(runUuid) with Logging {

	println("Parsing log file(s)...")

	val inputFiles = simulationLogDirectory(runUuid, create = false).files
		.collect { case file if (file.name.matches(FileDataReader.simulationFilesNamePattern)) => file.jfile }
		.toList

	logger.info(s"Collected $inputFiles from $runUuid")
	require(!inputFiles.isEmpty, "simulation directory doesn't contain any log file.")

	private def doWithInputFiles[T](f: Iterator[String] => T): T = {

		def multipleFileIterator(streams: Seq[InputStream]): Iterator[String] = streams.map(Source.fromInputStream(_, configuration.core.encoding).getLines).reduce((first, second) => first ++ second)

		val streams = inputFiles.map(new FileInputStream(_))
		try f(multipleFileIterator(streams))
		finally streams.foreach(_.close)
	}

	private def firstPass(records: Iterator[String]) = {

		logger.info("First pass")

		var count = 0
		var runStart = Long.MaxValue
		var runEnd = Long.MinValue
		val runRecords = mutable.ListBuffer.empty[RunRecord]

		records.foreach { line =>
			count += 1
			if (count % FileDataReader.logStep == 0) logger.info(s"First pass, read $count lines")

			line match {
				case ActionRecordType(array) =>
					runStart = math.min(runStart, array(4).toLong)
					runEnd = math.max(runEnd, array(7).toLong)

				case ScenarioRecordType(array) =>
					val time = array(4).toLong
					runStart = math.min(runStart, time)
					runEnd = math.max(runEnd, time)

				case RunRecordType(array) =>
					runRecords += RunRecord(parseTimestampString(array(1)), array(2), array(3).trim)
				case _ =>
			}
		}

		logger.info(s"First pass done: read $count lines")

		(runStart, runEnd, runRecords.head)
	}

	val (runStart, runEnd, runRecord) = doWithInputFiles(firstPass)

	val step = StatsHelper.step(math.floor(runStart / FileDataReader.secMillisecRatio).toInt, math.ceil(runEnd / FileDataReader.secMillisecRatio).toInt, configuration.charting.maxPlotsPerSeries) * FileDataReader.secMillisecRatio
	val bucketFunction = StatsHelper.bucket(_: Int, 0, (runEnd - runStart).toInt, step, step / 2)
	val buckets = StatsHelper.bucketsList(0, (runEnd - runStart).toInt, step)

	private def secondPass(bucketFunction: Int => Int)(records: Iterator[String]): ResultsHolder = {

		logger.info("Second pass")

		val resultsHolder = new ResultsHolder(runStart, runEnd)

		var count = 0

		records
			.foreach { line =>
				count += 1
				if (count % FileDataReader.logStep == 0) logger.info(s"Second pass, read $count lines")

				line match {
					case ActionRecordType(array) => resultsHolder.addActionRecord(ActionRecord(array, bucketFunction, runStart))
					case GroupRecordType(array) => resultsHolder.addGroupRecord(GroupRecord(array, bucketFunction, runStart))
					case ScenarioRecordType(array) => resultsHolder.addScenarioRecord(ScenarioRecord(array, bucketFunction, runStart))
					case _ =>
				}
			}

		logger.info(s"Second pass: read $count lines")

		resultsHolder
	}

	val resultsHolder = doWithInputFiles(secondPass(bucketFunction))

	println("Parsing log file(s) done")

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
		.map(plot => plot.copy(value = (plot.value / step * FileDataReader.secMillisecRatio).toInt))
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

		def process(buffer: Seq[IntVsTimePlot]): Seq[IntVsTimePlot] = {

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