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
package com.excilys.ebi.gatling.metrics.actors

import java.util.{ HashMap, Timer, TimerTask }

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.mutable

import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.{ RequestRecord, RunRecord, ShortScenarioDescription }
import com.excilys.ebi.gatling.core.result.writer.DataWriter
import com.excilys.ebi.gatling.metrics.reporting.GatlingGraphiteReporter

import akka.actor.{ ActorRef, Props }
import grizzled.slf4j.Logging

case object ClearHistograms

class MetricsDataWriter extends DataWriter with Logging {

	private val metricsActors: mutable.Map[String, ActorRef] = new HashMap[String, ActorRef]
	private val globalMetrics = context.actorOf(Props(new RequestMetrics("global")))
	private val timer = new Timer(true)

	def onInitializeDataWriter(runRecord: RunRecord, scenarios: Seq[ShortScenarioDescription]) {
		timer.scheduleAtFixedRate(new ClearTask, 0, 1000)
		GatlingGraphiteReporter.enable(configuration.graphite.period, configuration.graphite.timeUnit, configuration.graphite.host, configuration.graphite.port)
	}

	def onRequestRecord(requestRecord: RequestRecord) {
		dispatchRecord(requestRecord)
	}

	def onFlushDataWriter {
		GatlingGraphiteReporter.shutdown
	}

	private def dispatchRecord(requestRecord: RequestRecord) {
		val requestName = requestRecord.requestName
		if (requestName != START_OF_SCENARIO && requestName != END_OF_SCENARIO) {
			val metric = metricsActors.getOrElseUpdate(requestName, context.actorOf(Props(new RequestMetrics(requestName))))
			metric ! requestRecord
		}
		globalMetrics ! requestRecord
	}
	private class ClearTask extends TimerTask {
		def run() {
			globalMetrics ! ClearHistograms
			metricsActors.values.foreach(_ ! ClearHistograms)
		}
	}
}
