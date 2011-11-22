/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.excilys.ebi.gatling.core.context.handler.CounterBasedIterationHandler
import com.excilys.ebi.gatling.core.context.handler.TimerBasedIterationHandler
import com.excilys.ebi.gatling.core.runner.Runner.runSim
import com.excilys.ebi.gatling.core.structure.ScenarioBuilder
import com.excilys.ebi.gatling.core.structure.ChainBuilder
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit

object Predef {
	implicit def toSimpleActionBuilder(contextFunction: (Context, Action) => Unit): SimpleActionBuilder = SimpleActionBuilder.toSimpleActionBuilder(contextFunction)
	implicit def toSimpleActionBuilder(contextFunction: Context => Unit): SimpleActionBuilder = SimpleActionBuilder.toSimpleActionBuilder(contextFunction)

	type CSVFeeder = feeder.CSVFeeder
	type SSVFeeder = feeder.SSVFeeder
	type TSVFeeder = feeder.TSVFeeder

	type Context = context.Context

	val MILLISECONDS = TimeUnit.MILLISECONDS
	val SECONDS = TimeUnit.SECONDS
	val NANOSECONDS = TimeUnit.NANOSECONDS
	val MICROSECONDS = TimeUnit.MICROSECONDS
	val MINUTES = TimeUnit.MINUTES
	val HOURS = TimeUnit.HOURS
	val DAYS = TimeUnit.DAYS

	def getCounterValue(context: Context, counterName: String): Int = CounterBasedIterationHandler.getCounterValue(context, counterName)
	def getTimerValue(context: Context, timerName: String): Long = TimerBasedIterationHandler.getTimerValue(context, timerName)

	def runSimFunction(startDate: String) = runSim(new DateTime(startDate))_
	def runSimFunction(startDate: DateTime) = runSim(startDate)_

	def scenario(scenarioName: String): ScenarioBuilder = ScenarioBuilder.scenario(scenarioName)
	def chain = ChainBuilder.chain
}
