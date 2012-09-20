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

import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder
import com.excilys.ebi.gatling.core.check.{ Check, CheckBuilder, ExtractorCheckBuilder, MatcherCheckBuilder }
import com.excilys.ebi.gatling.core.feeder.{ Feeder, FeederBuiltIns }
import com.excilys.ebi.gatling.core.feeder.csv.SeparatedValuesParser
import com.excilys.ebi.gatling.core.structure.{ ChainBuilder, ScenarioBuilder }
import com.excilys.ebi.gatling.core.util.StringHelper.parseEvaluatable

object Predef {
	implicit def stringToSessionFunction(string: String) = parseEvaluatable(string)
	implicit def toSessionFunction[X](x: X) = (session: Session) => x
	implicit def checkBuilderToCheck[C <: Check[R, XC], R, XC](checkBuilder: CheckBuilder[C, R, XC]) = checkBuilder.build
	implicit def matcherCheckBuilderToCheckBuilder[C <: Check[R, XC], R, XC, X](matcherCheckBuilder: MatcherCheckBuilder[C, R, XC, X]) = matcherCheckBuilder.exists
	implicit def matcherCheckBuilderToCheck[C <: Check[R, XC], R, XC, X](matcherCheckBuilder: MatcherCheckBuilder[C, R, XC, X]) = matcherCheckBuilder.exists.build
	implicit def extractorCheckBuilderToMatcherCheckBuilder[C <: Check[R, XC], R, XC, X](extractorCheckBuilder: ExtractorCheckBuilder[C, R, XC, X]) = extractorCheckBuilder.find
	implicit def extractorCheckBuilderToCheckBuilder[C <: Check[R, XC], R, XC, X](extractorCheckBuilder: ExtractorCheckBuilder[C, R, XC, X]) = extractorCheckBuilder.find.exists
	implicit def extractorCheckBuilderToCheck[C <: Check[R, XC], R, XC, X](extractorCheckBuilder: ExtractorCheckBuilder[C, R, XC, X]) = extractorCheckBuilder.find.exists.build

	def csv(fileName: String) = SeparatedValuesParser.csv(fileName, None)
	def csv(fileName: String, escapeChar: String) = SeparatedValuesParser.csv(fileName, Some(escapeChar))
	def csv(file: File) = SeparatedValuesParser.csv(file, None)
	def csv(file: File, escapeChar: String) = SeparatedValuesParser.csv(file, Some(escapeChar))
	def ssv(fileName: String) = SeparatedValuesParser.ssv(fileName, None)
	def ssv(fileName: String, escapeChar: String) = SeparatedValuesParser.ssv(fileName, Some(escapeChar))
	def ssv(file: File) = SeparatedValuesParser.ssv(file, None)
	def ssv(file: File, escapeChar: String) = SeparatedValuesParser.ssv(file, Some(escapeChar))
	def tsv(fileName: String) = SeparatedValuesParser.tsv(fileName, None)
	def tsv(fileName: String, escapeChar: String) = SeparatedValuesParser.tsv(fileName, Some(escapeChar))
	def tsv(file: File) = SeparatedValuesParser.tsv(file, None)
	def tsv(file: File, escapeChar: String) = SeparatedValuesParser.tsv(file, Some(escapeChar))

	implicit def dataToFeeder(data: Array[Map[String, String]]): Feeder = dataToFeederBuiltIns(data).queue
	implicit def dataToFeederBuiltIns(data: Array[Map[String, String]]) = new FeederBuiltIns(data)

	type Session = com.excilys.ebi.gatling.core.session.Session
	type Simulation = com.excilys.ebi.gatling.core.scenario.configuration.Simulation
	type Feeder = com.excilys.ebi.gatling.core.feeder.Feeder

	@deprecated("Will be removed in Gatling 1.4.0.", "1.3.0")
	val MILLISECONDS = TimeUnit.MILLISECONDS
	@deprecated("Will be removed in Gatling 1.4.0.", "1.3.0")
	val SECONDS = TimeUnit.SECONDS
	@deprecated("Will be removed in Gatling 1.4.0.", "1.3.0")
	val NANOSECONDS = TimeUnit.NANOSECONDS
	@deprecated("Will be removed in Gatling 1.4.0.", "1.3.0")
	val MICROSECONDS = TimeUnit.MICROSECONDS
	@deprecated("Will be removed in Gatling 1.4.0.", "1.3.0")
	val MINUTES = TimeUnit.MINUTES
	@deprecated("Will be removed in Gatling 1.4.0.", "1.3.0")
	val HOURS = TimeUnit.HOURS
	@deprecated("Will be removed in Gatling 1.4.0.", "1.3.0")
	val DAYS = TimeUnit.DAYS

	def scenario(scenarioName: String): ScenarioBuilder = ScenarioBuilder.scenario(scenarioName)
	val bootstrap = ChainBuilder.emptyChain
	@deprecated("""Will be removed in Gatling 1.4.0. Use "import bootstrap._" instead.""", "1.3.0")
	val chain = bootstrap
}
