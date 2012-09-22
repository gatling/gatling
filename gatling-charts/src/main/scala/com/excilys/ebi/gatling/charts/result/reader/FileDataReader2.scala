package com.excilys.ebi.gatling.charts.result.reader

import java.io.File
import java.util.{ HashMap => JHashMap }
import java.util.regex.Pattern

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.mutable
import scala.io.Source

import com.excilys.ebi.gatling.charts.result.reader.processors.PreProcessor
import com.excilys.ebi.gatling.charts.result.reader.stats.StatsHelper
import com.excilys.ebi.gatling.core.action.{ EndAction, StartAction }
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogDirectory
import com.excilys.ebi.gatling.core.result.message.{ RecordType, RequestStatus }
import com.excilys.ebi.gatling.core.result.message.RequestStatus.RequestStatus
import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.core.result.reader.{ DataReader, GeneralStats }
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR

import grizzled.slf4j.Logging

object FileDataReader2 {
	val TABULATION_PATTERN = Pattern.compile(TABULATION_SEPARATOR)
	val SIMULATION_FILES_NAME_PATTERN = """.*\.log"""
}

class FileDataReader2(runUuid: String) extends DataReader(runUuid) with Logging {

	private def multipleFileIterator(files: Seq[File]): Iterator[String] = files.map(file => Source.fromFile(file, configuration.simulation.encoding).getLines()).reduce((first, second) => first ++ second)

	val inputFiles = simulationLogDirectory(runUuid, create = false).files.filter(_.jfile.getName.matches(FileDataReader2.SIMULATION_FILES_NAME_PATTERN)).map(_.jfile).toSeq

	if (inputFiles.isEmpty) throw new IllegalArgumentException("simulation directory doesn't contain any log file.")

	val (maxTime, minTime, step, size, runRecords) = PreProcessor.run(multipleFileIterator(inputFiles), configuration.charting.maxPlotsPerSeries)
	val bucketFunction = StatsHelper.bucket(_: Long, minTime, maxTime, step, step / 2)
	val buckets = StatsHelper.bucketsList(minTime, maxTime, step)

	val resultsHolder = new ResultsHolder

	multipleFileIterator(inputFiles)
		.filter(_.startsWith(RecordType.ACTION))
		.map(FileDataReader2.TABULATION_PATTERN.split(_))
		.filter(_.size >= 9)
		.map(ActionRecord(_))
		.foreach { record =>
			record.request match {
				case StartAction.START_OF_SCENARIO =>
					resultsHolder.addStartSessionBuffers(record)
				case EndAction.END_OF_SCENARIO =>
					resultsHolder.addEndSessionBuffers(record)
				case _ =>
					resultsHolder.updateRequestsPerSecBuffers(record)
					resultsHolder.updateTransactionsPerSecBuffers(record)
					resultsHolder.updateResponseTimePerSecBuffers(record)
					resultsHolder.updateLatencyPerSecBuffers(record)
					resultsHolder.addNames(record)
					resultsHolder.updateGeneralStatsBuffers(record)
					resultsHolder.updateResponseTimeRangeBuffer(record)
			}
		}

	object ActionRecord {

		def apply(strings: Array[String]) = new ActionRecord(strings(1), strings(3), strings(4).toLong, strings(5).toLong, strings(6).toLong, strings(7).toLong, RequestStatus.withName(strings(8)))
	}

	class ActionRecord(val scenario: String, val request: String, val executionStart: Long, val executionEnd: Long, val requestEnd: Long, val responseStart: Long, val status: RequestStatus) {
		val executionStartBucket = bucketFunction(executionStart)
		val executionEndBucket = bucketFunction(executionEnd)
		val responseTime = executionEnd - executionStart
		val latency = responseStart - requestEnd
	}

	class CountBuffer {
		// FIXME add canBuildFrom
		val map = new JHashMap[Long, Long]

		def increment(bucket: Long) {
			if (map.containsKey(bucket)) {
				val currentValue = map.get(bucket)
				map.put(bucket, currentValue + 1)
			} else {
				map.put(bucket, 1L)
			}
		}
	}

	class SessionDeltaBuffer {
		val map = new JHashMap[Long, (Long, Long)]

		def addStart(bucket: Long) {
			if (map.containsKey(bucket)) {
				val (start, end) = map.get(bucket)
				map.put(bucket, (start + 1, end))
			} else {
				map.put(bucket, (1, 0))
			}
		}

		def addEnd(bucket: Long) {
			if (map.containsKey(bucket)) {
				val (start, end) = map.get(bucket)
				map.put(bucket, (start, end + 1))
			} else {
				map.put(bucket, (0, 1))
			}
		}
		def compute(buckets: List[Long]): List[(Long, Long)] = {

			val (_, _, sessions) = buckets.foldLeft(0L, 0L, List.empty[(Long, Long)]) { (accumulator, bucket) =>
				val (previousSessions, previousEnds, sessions) = accumulator
				val (bucketStarts, bucketEnds) = map.getOrElse(bucket, (0L, 0L))
				val bucketSessions = previousSessions - previousEnds + bucketStarts
				(bucketSessions, bucketEnds, (bucket, bucketSessions) :: sessions)
			}

			sessions.reverse
		}
	}

	class RangeBuffer {
		// FIXME add canBuildFrom
		val map = new JHashMap[Long, (Long, Long)]

		def update(bucket: Long, value: Long) {
			if (map.containsKey(bucket)) {
				val (minValue, maxValue) = map.get(bucket)
				map.put(bucket, (value min minValue, value max maxValue))
			} else {
				map.put(bucket, (value, value))
			}
		}
	}

	class NameBuffer {
		// FIXME add canBuildFrom
		val map = new mutable.HashMap[String, Long]

		def update(name: String, time: Long) {

			val minTime = map.getOrElseUpdate(name, time)
			if (time < minTime)
				map.put(name, time)
		}
	}

	class GeneralStatsBuffer {
		// maybe use an ArrayList here
		val times = mutable.ArrayBuffer[Long]()

		def update(time: Long) {
			times += time
		}

		def compute: GeneralStats = {

			if (times.isEmpty) {
				GeneralStats(-1, -1, 0, -1, -1, -1, -1, -1)
			} else {
				val (min, max) = times.foldLeft((Long.MaxValue, Long.MinValue)) { (current, time) =>
					val (currentMin, currentMax) = current
					val newMin = if (time < currentMin) time else currentMin
					val newMax = if (time > currentMax) time else currentMax
					(newMin, newMax)
				}

				val count = times.size
				val sum = times.sum
				val meanResponseTime = math.round(sum / count.toDouble)
				val meanRequestsPerSec = math.round(count / ((maxTime - minTime) / 1000.0))
				val squareSum = times.foldLeft(0L) { (sum, responseTime) =>
					val diff = responseTime - meanResponseTime
					sum + diff * diff
				}
				val stdDev = math.round(math.sqrt(squareSum / count))

				val sortedTimes = times.sorted
				val percentile1 = sortedTimes(((configuration.charting.indicators.percentile1 / 100.0) * count).toInt)
				val percentile2 = sortedTimes(((configuration.charting.indicators.percentile2 / 100.0) * count).toInt)

				{

				}

				GeneralStats(min, max, count, meanResponseTime, stdDev, percentile1, percentile2, meanRequestsPerSec)
			}
		}
	}

	class ResponseTimeRangeBuffer {

		var low = 0
		var middle = 0
		var high = 0
		var ko = 0

		def update(record: ActionRecord) {

			if (record.status == RequestStatus.KO) ko += 1
			else if (record.responseTime < configuration.charting.indicators.lowerBound) low += 1
			else if (record.responseTime > configuration.charting.indicators.higherBound) high += 1
			else middle += 1
		}
	}

	type BufferKey = (Option[String], Option[RequestStatus.RequestStatus])

	class ResultsHolder {
		val requestsPerSecBuffers = new JHashMap[BufferKey, CountBuffer]
		val transactionsPerSecBuffers = new JHashMap[BufferKey, CountBuffer]
		val responseTimePerSecBuffers = new JHashMap[BufferKey, RangeBuffer]
		val latencyPerSecBuffers = new JHashMap[BufferKey, RangeBuffer]
		val sessionDeltaPerSecBuffers = new JHashMap[BufferKey, SessionDeltaBuffer]
		val requestNameBuffer = new NameBuffer
		val scenarioNameBuffer = new NameBuffer
		val generalStatsBuffer = new JHashMap[BufferKey, GeneralStatsBuffer]
		val responseTimeRangeBuffers = new JHashMap[BufferKey, ResponseTimeRangeBuffer]

		private def computeKey(requestName: Option[String], status: Option[RequestStatus.RequestStatus]): BufferKey = (requestName, status)

		private def getBuffer[B](key: BufferKey, buffers: JHashMap[BufferKey, B], builder: () => B): B = {
			if (buffers.containsKey(key))
				buffers.get(key)
			else {
				val buffer = builder()
				buffers.put(key, buffer)
				buffer
			}
		}

		def getRequestsPerSecBuffer(requestName: Option[String], status: Option[RequestStatus.RequestStatus]): CountBuffer = getBuffer(computeKey(requestName, status), requestsPerSecBuffers, () => new CountBuffer)
		def getTransactionsPerSecBuffer(requestName: Option[String], status: Option[RequestStatus.RequestStatus]): CountBuffer = getBuffer(computeKey(requestName, status), transactionsPerSecBuffers, () => new CountBuffer)
		def getResponseTimePerSecBuffers(requestName: Option[String], status: Option[RequestStatus.RequestStatus]): RangeBuffer = getBuffer(computeKey(requestName, status), responseTimePerSecBuffers, () => new RangeBuffer)
		def getLatencyPerSecBuffers(requestName: Option[String], status: Option[RequestStatus.RequestStatus]): RangeBuffer = getBuffer(computeKey(requestName, status), latencyPerSecBuffers, () => new RangeBuffer)
		def getSessionDeltaPerSecBuffers(scenarioName: Option[String]): SessionDeltaBuffer = getBuffer(computeKey(scenarioName, None), sessionDeltaPerSecBuffers, () => new SessionDeltaBuffer)
		def getGeneralStatsBuffers(requestName: Option[String], status: Option[RequestStatus.RequestStatus]): GeneralStatsBuffer = getBuffer(computeKey(requestName, status), generalStatsBuffer, () => new GeneralStatsBuffer)
		def getResponseTimeRangeBuffers(requestName: Option[String]): ResponseTimeRangeBuffer = getBuffer(computeKey(requestName, None), responseTimeRangeBuffers, () => new ResponseTimeRangeBuffer)

		def updateRequestsPerSecBuffers(record: ActionRecord) {
			getRequestsPerSecBuffer(None, None).increment(record.executionStartBucket)
			getRequestsPerSecBuffer(None, Some(record.status)).increment(record.executionStartBucket)
			getRequestsPerSecBuffer(Some(record.request), None).increment(record.executionStartBucket)
			getRequestsPerSecBuffer(Some(record.request), Some(record.status)).increment(record.executionStartBucket)
		}

		def updateTransactionsPerSecBuffers(record: ActionRecord) {
			getTransactionsPerSecBuffer(None, None).increment(record.executionEndBucket)
			getTransactionsPerSecBuffer(None, Some(record.status)).increment(record.executionEndBucket)
			getTransactionsPerSecBuffer(Some(record.request), None).increment(record.executionEndBucket)
			getTransactionsPerSecBuffer(Some(record.request), Some(record.status)).increment(record.executionEndBucket)
		}

		def updateResponseTimePerSecBuffers(record: ActionRecord) {
			getResponseTimePerSecBuffers(Some(record.request), Some(record.status)).update(record.executionStartBucket, record.responseTime)
		}

		def updateLatencyPerSecBuffers(record: ActionRecord) {
			getLatencyPerSecBuffers(Some(record.request), Some(record.status)).update(record.executionStartBucket, record.latency)
		}

		def addStartSessionBuffers(record: ActionRecord) {
			getSessionDeltaPerSecBuffers(None).addStart(record.executionStartBucket)
			getSessionDeltaPerSecBuffers(Some(record.scenario)).addStart(record.executionStartBucket)
		}

		def addEndSessionBuffers(record: ActionRecord) {
			getSessionDeltaPerSecBuffers(None).addEnd(record.executionStartBucket)
			getSessionDeltaPerSecBuffers(Some(record.scenario)).addEnd(record.executionStartBucket)
		}

		def addNames(record: ActionRecord) {
			requestNameBuffer.update(record.request, record.executionStart)
			scenarioNameBuffer.update(record.scenario, record.executionStart)
		}

		def updateGeneralStatsBuffers(record: ActionRecord) {
			getGeneralStatsBuffers(None, None).update(record.responseTime)
			getGeneralStatsBuffers(None, Some(record.status)).update(record.responseTime)
			getGeneralStatsBuffers(Some(record.request), None).update(record.responseTime)
			getGeneralStatsBuffers(Some(record.request), Some(record.status)).update(record.responseTime)
		}

		def updateResponseTimeRangeBuffer(record: ActionRecord) {
			getResponseTimeRangeBuffers(None).update(record)
			getResponseTimeRangeBuffers(Some(record.request)).update(record)
		}
	}

	def runRecord: RunRecord = runRecords.head

	def requestNames: Seq[String] = resultsHolder.requestNameBuffer.map.toList.sortBy(_._2).map(_._1)

	def scenarioNames: Seq[String] = resultsHolder.scenarioNameBuffer.map.toList.sortBy(_._2).map(_._1)

	def numberOfActiveSessionsPerSecond(scenarioName: Option[String]): Seq[(Long, Long)] = resultsHolder.getSessionDeltaPerSecBuffers(scenarioName).compute(buckets)

	def numberOfRequestsPerSecond(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Seq[(Long, Long)] = resultsHolder
		.getRequestsPerSecBuffer(requestName, status).map
		.toList
		.map { case (bucket, count) => (bucket, math.round(count / step * 1000)) }
		.sorted

	def numberOfTransactionsPerSecond(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): Seq[(Long, Long)] = resultsHolder
		.getTransactionsPerSecBuffer(requestName, status).map
		.toList
		.map { case (bucket, count) => (bucket, math.round(count / step * 1000)) }
		.sorted

	def responseTimeDistribution(slotsNumber: Int, requestName: Option[String]): (Seq[(Long, Long)], Seq[(Long, Long)]) = null

	def generalStats(status: Option[RequestStatus.RequestStatus], requestName: Option[String]): GeneralStats = resultsHolder.getGeneralStatsBuffers(requestName, status).compute

	def numberOfRequestInResponseTimeRange(lowerBound: Int, higherBound: Int, requestName: Option[String]): Seq[(String, Long)] = {

		val counts = resultsHolder.getResponseTimeRangeBuffers(requestName)

		List(("t < " + lowerBound + " ms", counts.low),
			(lowerBound + " ms < t < " + higherBound + " ms", counts.middle),
			("t > " + higherBound + " ms", counts.high),
			("failed", counts.ko))
	}

	def responseTimeGroupByExecutionStartDate(status: RequestStatus.RequestStatus, requestName: String): Seq[(Long, (Long, Long))] = resultsHolder.getResponseTimePerSecBuffers(Some(requestName), Some(status)).map.toList.sorted

	def latencyGroupByExecutionStartDate(status: RequestStatus.RequestStatus, requestName: String): Seq[(Long, (Long, Long))] = resultsHolder.getLatencyPerSecBuffers(Some(requestName), Some(status)).map.toList.sorted

	def responseTimeAgainstGlobalNumberOfRequestsPerSec(status: RequestStatus.RequestStatus, requestName: String): Seq[(Long, Long)] = {

		val globalCountsByBucket: JHashMap[Long, Long] = resultsHolder.getRequestsPerSecBuffer(None, None).map
		val responseTimeByBucket: JHashMap[Long, (Long, Long)] = resultsHolder.getResponseTimePerSecBuffers(Some(requestName), Some(status)).map

		responseTimeByBucket
			.toList
			.map {
				case (bucket, responseTimes) =>
					val (min, max) = responseTimes
					val count = globalCountsByBucket.get(bucket)
					(math.round(count / step * 1000), max)
			}.sorted
	}
}