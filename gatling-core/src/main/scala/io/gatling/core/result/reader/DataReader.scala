/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.result.reader

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.{ ErrorStats, Group, IntRangeVsTimePlot, IntVsTimePlot, StatsPath }
import io.gatling.core.result.message.Status
import io.gatling.core.result.writer.RunMessage

object DataReader {
	val NO_PLOT_MAGIC_VALUE = -1

	def newInstance(runOn: String) = Class.forName(configuration.data.dataReaderClass).asInstanceOf[Class[DataReader]].getConstructor(classOf[String]).newInstance(runOn)
}

abstract class DataReader(runUuid: String) {

	def runMessage: RunMessage
	def runStart: Long
	def runEnd: Long
	def statsPaths: List[StatsPath]
	def scenarioNames: List[String]
	def numberOfActiveSessionsPerSecond(scenarioName: Option[String] = None): Seq[IntVsTimePlot]
	def numberOfRequestsPerSecond(status: Option[Status] = None, requestName: Option[String] = None, group: Option[Group] = None): Seq[IntVsTimePlot]
	def numberOfResponsesPerSecond(status: Option[Status] = None, requestName: Option[String] = None, group: Option[Group] = None): Seq[IntVsTimePlot]
	def responseTimeDistribution(slotsNumber: Int, requestName: Option[String] = None, group: Option[Group] = None): (Seq[IntVsTimePlot], Seq[IntVsTimePlot])
	def requestGeneralStats(requestName: Option[String] = None, group: Option[Group] = None, status: Option[Status] = None): GeneralStats
	def numberOfRequestInResponseTimeRange(requestName: Option[String] = None, group: Option[Group] = None): Seq[(String, Int)]
	def responseTimeGroupByExecutionStartDate(status: Status, requestName: String, group: Option[Group] = None): Seq[IntRangeVsTimePlot]
	def latencyGroupByExecutionStartDate(status: Status, requestName: String, group: Option[Group] = None): Seq[IntRangeVsTimePlot]
	def responseTimeAgainstGlobalNumberOfRequestsPerSec(status: Status, requestName: String, group: Option[Group] = None): Seq[IntVsTimePlot]

	def errors(requestName: Option[String], group: Option[Group]): Seq[ErrorStats]

	def groupCumulatedResponseTimeGeneralStats(group: Group, status: Option[Status]): GeneralStats
	def groupDurationGeneralStats(group: Group, status: Option[Status]): GeneralStats
	def groupCumulatedResponseTimeDistribution(slotsNumber: Int, group: Group): (Seq[IntVsTimePlot], Seq[IntVsTimePlot])
	def groupDurationDistribution(slotsNumber: Int, group: Group): (Seq[IntVsTimePlot], Seq[IntVsTimePlot])
	def groupCumulatedResponseTimeGroupByExecutionStartDate(status: Status, group: Group): Seq[IntRangeVsTimePlot]
	def groupDurationGroupByExecutionStartDate(status: Status, group: Group): Seq[IntRangeVsTimePlot]
}
