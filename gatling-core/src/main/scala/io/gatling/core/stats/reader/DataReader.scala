/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.stats.reader

import io.gatling.core.assertion.Assertion
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.stats._
import io.gatling.core.stats.message.Status
import io.gatling.core.stats.writer.RunMessage

object DataReader {
  val NoPlotMagicValue = -1

  def newInstance(runOn: String)(implicit configuration: GatlingConfiguration) =
    Class.forName(configuration.data.dataReaderClass)
      .asInstanceOf[Class[DataReader]]
      .getConstructor(classOf[String], classOf[GatlingConfiguration])
      .newInstance(runOn, configuration)
}

abstract class DataReader(runUuid: String) {

  def runMessage: RunMessage
  def runStart: Long
  def runEnd: Long
  def assertions: List[Assertion]
  def statsPaths: List[StatsPath]
  def requestNames: List[String]
  def scenarioNames: List[String]
  def numberOfActiveSessionsPerSecond(scenarioName: Option[String] = None): Seq[IntVsTimePlot]
  def numberOfRequestsPerSecond(requestName: Option[String] = None, group: Option[Group] = None): Seq[CountsVsTimePlot]
  def numberOfResponsesPerSecond(requestName: Option[String] = None, group: Option[Group] = None): Seq[CountsVsTimePlot]
  def responseTimeDistribution(maxPlots: Int, requestName: Option[String] = None, group: Option[Group] = None): (Seq[PercentVsTimePlot], Seq[PercentVsTimePlot])
  def requestGeneralStats(requestName: Option[String] = None, group: Option[Group] = None, status: Option[Status] = None): GeneralStats
  def numberOfRequestInResponseTimeRange(requestName: Option[String] = None, group: Option[Group] = None): Seq[(String, Int)]
  def responseTimePercentilesOverTime(status: Status, requestName: Option[String], group: Option[Group]): Iterable[PercentilesVsTimePlot]
  def latencyPercentilesOverTime(status: Status, requestName: Option[String], group: Option[Group]): Iterable[PercentilesVsTimePlot]
  def responseTimeAgainstGlobalNumberOfRequestsPerSec(status: Status, requestName: String, group: Option[Group] = None): Seq[IntVsTimePlot]
  def latencyAgainstGlobalNumberOfRequestsPerSec(status: Status, requestName: String, group: Option[Group] = None): Seq[IntVsTimePlot]

  def errors(requestName: Option[String], group: Option[Group]): Seq[ErrorStats]

  def groupCumulatedResponseTimeGeneralStats(group: Group, status: Option[Status]): GeneralStats
  def groupDurationGeneralStats(group: Group, status: Option[Status]): GeneralStats
  def groupCumulatedResponseTimeDistribution(maxPlots: Int, group: Group): (Seq[PercentVsTimePlot], Seq[PercentVsTimePlot])
  def groupDurationDistribution(maxPlots: Int, group: Group): (Seq[PercentVsTimePlot], Seq[PercentVsTimePlot])
  def groupCumulatedResponseTimePercentilesOverTime(status: Status, group: Group): Iterable[PercentilesVsTimePlot]
  def groupDurationPercentilesOverTime(status: Status, group: Group): Iterable[PercentilesVsTimePlot]
}
