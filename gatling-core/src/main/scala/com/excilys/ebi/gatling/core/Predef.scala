/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core

import com.excilys.ebi.gatling.core._
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder
import com.excilys.ebi.gatling.core.session.handler.CounterBasedIterationHandler
import com.excilys.ebi.gatling.core.session.handler.TimerBasedIterationHandler
import com.excilys.ebi.gatling.core.runner.Runner.runSim
import com.excilys.ebi.gatling.core.structure.ScenarioBuilder
import com.excilys.ebi.gatling.core.structure.ChainBuilder
import com.excilys.ebi.gatling.core.feeder.csv.SeparatedValuesFeederBuilder
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit

object Predef {
	implicit def toSimpleActionBuilder(sessionFunction: (Session, Action) => Unit): SimpleActionBuilder = SimpleActionBuilder.toSimpleActionBuilder(sessionFunction)
	implicit def toSimpleActionBuilder(sessionFunction: Session => Unit): SimpleActionBuilder = SimpleActionBuilder.toSimpleActionBuilder(sessionFunction)

	def csv(fileName: String) = SeparatedValuesFeederBuilder.csv(fileName)
	def ssv(fileName: String) = SeparatedValuesFeederBuilder.ssv(fileName)
	def tsv(fileName: String) = SeparatedValuesFeederBuilder.tsv(fileName)

	type Session = session.Session

	val MILLISECONDS = TimeUnit.MILLISECONDS
	val SECONDS = TimeUnit.SECONDS
	val NANOSECONDS = TimeUnit.NANOSECONDS
	val MICROSECONDS = TimeUnit.MICROSECONDS
	val MINUTES = TimeUnit.MINUTES
	val HOURS = TimeUnit.HOURS
	val DAYS = TimeUnit.DAYS

	def getCounterValue(session: Session, counterName: String): Int = CounterBasedIterationHandler.getCounterValue(session, counterName)
	def getTimerValue(session: Session, timerName: String): Long = TimerBasedIterationHandler.getTimerValue(session, timerName)

	def runSimFunction(startDate: String) = runSim(new DateTime(startDate))_
	def runSimFunction(startDate: DateTime) = runSim(startDate)_

	def scenario(scenarioName: String): ScenarioBuilder = ScenarioBuilder.scenario(scenarioName)
	def chain = ChainBuilder.chain
}
