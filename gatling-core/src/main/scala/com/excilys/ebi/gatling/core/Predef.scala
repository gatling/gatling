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

import scala.tools.nsc.io.File

import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder.simpleActionBuilder
import com.excilys.ebi.gatling.core.check.{ Check, CheckBuilder, ExtractorCheckBuilder, MatcherCheckBuilder }
import com.excilys.ebi.gatling.core.feeder.FeederSource
import com.excilys.ebi.gatling.core.feeder.csv.SeparatedValuesFeederSource
import com.excilys.ebi.gatling.core.feeder.simple.SimpleFeederBuilder
import com.excilys.ebi.gatling.core.structure.{ ChainBuilder, ScenarioBuilder }
import com.excilys.ebi.gatling.core.util.StringHelper.parseEvaluatable

object Predef {
	implicit def sessionFunctionToSimpleActionBuilder(sessionFunction: Session => Session) = simpleActionBuilder(sessionFunction)
	implicit def stringToSessionFunction(string: String) = parseEvaluatable(string)
	implicit def toSessionFunction[X](x: X) = (session: Session) => x
	implicit def checkBuilderToCheck[C <: Check[R, XC], R, XC](checkBuilder: CheckBuilder[C, R, XC]) = checkBuilder.build
	implicit def matcherCheckBuilderToCheckBuilder[C <: Check[R, XC], R, XC, X](matcherCheckBuilder: MatcherCheckBuilder[C, R, XC, X]) = matcherCheckBuilder.exists
	implicit def matcherCheckBuilderToCheck[C <: Check[R, XC], R, XC, X](matcherCheckBuilder: MatcherCheckBuilder[C, R, XC, X]) = matcherCheckBuilder.exists.build
	implicit def extractorCheckBuilderToMatcherCheckBuilder[C <: Check[R, XC], R, XC, X](extractorCheckBuilder: ExtractorCheckBuilder[C, R, XC, X]) = extractorCheckBuilder.find
	implicit def extractorCheckBuilderToCheckBuilder[C <: Check[R, XC], R, XC, X](extractorCheckBuilder: ExtractorCheckBuilder[C, R, XC, X]) = extractorCheckBuilder.find.exists
	implicit def extractorCheckBuilderToCheck[C <: Check[R, XC], R, XC, X](extractorCheckBuilder: ExtractorCheckBuilder[C, R, XC, X]) = extractorCheckBuilder.find.exists.build

	def csv(fileName: String): FeederSource = SeparatedValuesFeederSource.csv(fileName, None)
	def csv(fileName: String, escapeChar: String): FeederSource = SeparatedValuesFeederSource.csv(fileName, Some(escapeChar))
	def csv(file: File): FeederSource = SeparatedValuesFeederSource.csv(file, None)
	def csv(file: File, escapeChar: String): FeederSource = SeparatedValuesFeederSource.csv(file, Some(escapeChar))
	def ssv(fileName: String): FeederSource = SeparatedValuesFeederSource.ssv(fileName, None)
	def ssv(fileName: String, escapeChar: String): FeederSource = SeparatedValuesFeederSource.ssv(fileName, Some(escapeChar))
	def ssv(file: File): FeederSource = SeparatedValuesFeederSource.ssv(file, None)
	def ssv(file: File, escapeChar: String): FeederSource = SeparatedValuesFeederSource.ssv(file, Some(escapeChar))
	def tsv(fileName: String): FeederSource = SeparatedValuesFeederSource.tsv(fileName, None)
	def tsv(fileName: String, escapeChar: String): FeederSource = SeparatedValuesFeederSource.tsv(fileName, Some(escapeChar))
	def tsv(file: File): FeederSource = SeparatedValuesFeederSource.tsv(file, None)
	def tsv(file: File, escapeChar: String): FeederSource = SeparatedValuesFeederSource.tsv(file, Some(escapeChar))

	def simpleFeeder(name: String, data: Map[String, String]*) = SimpleFeederBuilder.simpleFeeder(name, data.toIndexedSeq)

	type Session = com.excilys.ebi.gatling.core.session.Session
	type Simulation = com.excilys.ebi.gatling.core.scenario.configuration.Simulation

	@deprecated("Will be removed in Gatling 1.4.0.")
	val MILLISECONDS = TimeUnit.MILLISECONDS
	@deprecated("Will be removed in Gatling 1.4.0.")
	val SECONDS = TimeUnit.SECONDS
	@deprecated("Will be removed in Gatling 1.4.0.")
	val NANOSECONDS = TimeUnit.NANOSECONDS
	@deprecated("Will be removed in Gatling 1.4.0.")
	val MICROSECONDS = TimeUnit.MICROSECONDS
	@deprecated("Will be removed in Gatling 1.4.0.")
	val MINUTES = TimeUnit.MINUTES
	@deprecated("Will be removed in Gatling 1.4.0.")
	val HOURS = TimeUnit.HOURS
	@deprecated("Will be removed in Gatling 1.4.0.")
	val DAYS = TimeUnit.DAYS

	def scenario(scenarioName: String): ScenarioBuilder = ScenarioBuilder.scenario(scenarioName)
	@deprecated("""Will be removed in Gatling 1.4.0. Use "emptyChain" instead.""")
	def chain = emptyChain
	def emptyChain = ChainBuilder.chain
}
