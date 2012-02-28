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

import java.util.concurrent.TimeUnit

import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.check.{ CheckOneBuilder, CheckMultipleBuilder, CheckBuilder, Check }
import com.excilys.ebi.gatling.core.check.CheckBaseBuilder
import com.excilys.ebi.gatling.core.feeder.csv.SeparatedValuesFeederBuilder
import com.excilys.ebi.gatling.core.session.handler.{ TimerBasedIterationHandler, CounterBasedIterationHandler }
import com.excilys.ebi.gatling.core.structure.{ ScenarioBuilder, ChainBuilder }
import com.excilys.ebi.gatling.core.util.StringHelper.parseEvaluatable

object Predef {
	implicit def toSimpleActionBuilder(sessionFunction: (Session, Action) => Session): SimpleActionBuilder = SimpleActionBuilder.toSimpleActionBuilder(sessionFunction)
	implicit def toSimpleActionBuilder(sessionFunction: Session => Session): SimpleActionBuilder = SimpleActionBuilder.toSimpleActionBuilder(sessionFunction)
	implicit def stringToSessionFunction(string: String) = parseEvaluatable(string)
	implicit def toSessionFunction[X](x: X) = (session: Session) => x
	implicit def checkWithVerifyBuilderToHttpCheck[C <: Check[R, X], R, X](builder: CheckBuilder[C, R, X]) = builder.build
	implicit def checkOneToExists[C <: Check[R, X], R, X](builder: CheckOneBuilder[C, R, X]) = builder.exists
	implicit def checkOneToCheck[C <: Check[R, X], R, X](builder: CheckOneBuilder[C, R, X]) = builder.exists.build
	implicit def checkMultipleToNotEmpty[C <: Check[R, Seq[X]], R, X](builder: CheckMultipleBuilder[C, R, Seq[X]]) = builder.notEmpty
	implicit def checkMultipleToCheck[C <: Check[R, Seq[X]], R, X](builder: CheckMultipleBuilder[C, R, Seq[X]]) = builder.notEmpty.build
	implicit def checkBuilderToCheckOne[C <: Check[R, X], R, X](builder: CheckBaseBuilder[C, R, X]) = builder.find
	implicit def checkBuilderToExists[C <: Check[R, X], R, X](builder: CheckBaseBuilder[C, R, X]) = builder.find.exists
	implicit def checkBuilderToCheck[C <: Check[R, X], R, X](builder: CheckBaseBuilder[C, R, X]) = builder.find.exists.build

	def csv(fileName: String) = SeparatedValuesFeederBuilder.csv(fileName)
	def csv(fileName: String, escapeChar: Char) = SeparatedValuesFeederBuilder.csv(fileName, Some(escapeChar))
	def ssv(fileName: String) = SeparatedValuesFeederBuilder.ssv(fileName)
	def ssv(fileName: String, escapeChar: Char) = SeparatedValuesFeederBuilder.ssv(fileName, Some(escapeChar))
	def tsv(fileName: String) = SeparatedValuesFeederBuilder.tsv(fileName)
	def tsv(fileName: String, escapeChar: Char) = SeparatedValuesFeederBuilder.tsv(fileName, Some(escapeChar))

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

	def scenario(scenarioName: String): ScenarioBuilder = ScenarioBuilder.scenario(scenarioName)
	def chain = ChainBuilder.chain
}
