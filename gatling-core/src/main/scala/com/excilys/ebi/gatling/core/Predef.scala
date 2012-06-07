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
import com.redis.RedisClientPool

import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder.simpleActionBuilder
import com.excilys.ebi.gatling.core.check.{ ExtractorCheckBuilder, CheckBuilder, Check }
import com.excilys.ebi.gatling.core.check.MatcherCheckBuilder
import com.excilys.ebi.gatling.core.feeder.csv.SeparatedValuesFeederBuilder
import com.excilys.ebi.gatling.core.session.handler.{ TimerBasedIterationHandler, CounterBasedIterationHandler }
import com.excilys.ebi.gatling.core.structure.{ ScenarioBuilder, ChainBuilder }
import com.excilys.ebi.gatling.core.util.StringHelper.parseEvaluatable

object Predef {
	implicit def sessionFunctionToSimpleActionBuilder(sessionFunction: Session => Session) = simpleActionBuilder(sessionFunction)
	implicit def stringToSessionFunction(string: String) = parseEvaluatable(string)
	implicit def toSessionFunction[X](x: X) = (session: Session) => x
	implicit def checkBuilderToCheck[C <: Check[R], R](checkBuilder: CheckBuilder[C, R]) = checkBuilder.build
	implicit def matcherCheckBuilderToCheckBuilder[C <: Check[R], R, X](matcherCheckBuilder: MatcherCheckBuilder[C, R, X]) = matcherCheckBuilder.exists
	implicit def matcherCheckBuilderToCheck[C <: Check[R], R, X](matcherCheckBuilder: MatcherCheckBuilder[C, R, X]) = matcherCheckBuilder.exists.build
	implicit def extractorCheckBuilderToMatcherCheckBuilder[C <: Check[R], R, X](extractorCheckBuilder: ExtractorCheckBuilder[C, R, X]) = extractorCheckBuilder.find
	implicit def extractorCheckBuilderToCheckBuilder[C <: Check[R], R, X](extractorCheckBuilder: ExtractorCheckBuilder[C, R, X]) = extractorCheckBuilder.find.exists
	implicit def extractorCheckBuilderToCheck[C <: Check[R], R, X](extractorCheckBuilder: ExtractorCheckBuilder[C, R, X]) = extractorCheckBuilder.find.exists.build

	def csv(fileName: String) = SeparatedValuesFeederBuilder.csv(fileName)
	def csv(fileName: String, escapeChar: Char) = SeparatedValuesFeederBuilder.csv(fileName, Some(escapeChar))
	def ssv(fileName: String) = SeparatedValuesFeederBuilder.ssv(fileName)
	def ssv(fileName: String, escapeChar: Char) = SeparatedValuesFeederBuilder.ssv(fileName, Some(escapeChar))
	def tsv(fileName: String) = SeparatedValuesFeederBuilder.tsv(fileName)
	def tsv(fileName: String, escapeChar: Char) = SeparatedValuesFeederBuilder.tsv(fileName, Some(escapeChar))
	
	type Session = com.excilys.ebi.gatling.core.session.Session
	type Simulation = com.excilys.ebi.gatling.core.scenario.configuration.Simulation

	val MILLISECONDS = TimeUnit.MILLISECONDS
	val SECONDS = TimeUnit.SECONDS
	val NANOSECONDS = TimeUnit.NANOSECONDS
	val MICROSECONDS = TimeUnit.MICROSECONDS
	val MINUTES = TimeUnit.MINUTES
	val HOURS = TimeUnit.HOURS
	val DAYS = TimeUnit.DAYS

	def scenario(scenarioName: String): ScenarioBuilder = ScenarioBuilder.scenario(scenarioName)
	def chain = ChainBuilder.chain
}
