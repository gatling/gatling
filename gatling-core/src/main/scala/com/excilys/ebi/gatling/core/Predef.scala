/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import scala.concurrent.duration.{ DurationInt, FiniteDuration }
import scala.reflect.ClassTag
import scala.tools.nsc.io.{ File, Path }

import com.excilys.ebi.gatling.core.check.{ Check, CheckBuilder, ExtractorCheckBuilder, MatcherCheckBuilder }
import com.excilys.ebi.gatling.core.feeder.{ AdvancedFeederBuilder, Feeder, FeederBuilder, FeederBuilderFromArray, FeederBuilderFromFeeder }
import com.excilys.ebi.gatling.core.feeder.csv.SeparatedValuesParser
import com.excilys.ebi.gatling.core.scenario.injection.{ DelayInjection, PeakInjection, RampInjection }
import com.excilys.ebi.gatling.core.session.{ ELCompiler, ELWrapper }
import com.excilys.ebi.gatling.core.structure.{ AssertionBuilder, ChainBuilder, ScenarioBuilder }
import com.excilys.ebi.gatling.core.validation.{ SuccessWrapper, Validation }

object Predef {
	implicit def stringToExpression[T: ClassTag](string: String) = string.el
	implicit def value2Success[T](value: T): Validation[T] = value.success
	implicit def value2Expression[T](value: T): Expression[T] = value.expression
	implicit def checkBuilder2Check[C <: Check[R], R, P, T, X, E](checkBuilder: CheckBuilder[C, R, P, T, X, E]) = checkBuilder.build
	implicit def matcherCheckBuilder2CheckBuilder[C <: Check[R], R, P, T, X](matcherCheckBuilder: MatcherCheckBuilder[C, R, P, T, X]) = matcherCheckBuilder.exists
	implicit def matcherCheckBuilder2Check[C <: Check[R], R, P, T, X](matcherCheckBuilder: MatcherCheckBuilder[C, R, P, T, X]) = matcherCheckBuilder.exists.build
	implicit def extractorCheckBuilder2MatcherCheckBuilder[C <: Check[R], R, P, T, X](extractorCheckBuilder: ExtractorCheckBuilder[C, R, P, T, X]) = extractorCheckBuilder.find
	implicit def extractorCheckBuilder2CheckBuilder[C <: Check[R], R, P, T, X](extractorCheckBuilder: ExtractorCheckBuilder[C, R, P, T, X]) = extractorCheckBuilder.find.exists
	implicit def extractorCheckBuilder2Check[C <: Check[R], R, P, T, X](extractorCheckBuilder: ExtractorCheckBuilder[C, R, P, T, X]) = extractorCheckBuilder.find.exists.build
	implicit def map2ExpressionMap(map: Map[String, Any]): Map[String, Expression[Any]] = map.mapValues(_ match {
		case string: String => string.el
		case any => any.expression
	})
	implicit def intToFiniteDuration(i: Int) = i seconds

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

	implicit def data2FeederBuilder[T](data: Array[Map[String, T]]): AdvancedFeederBuilder[T] = FeederBuilderFromArray(data)
	implicit def feeder2FeederBuilder[T](feeder: Feeder[T]): FeederBuilder[T] = FeederBuilderFromFeeder(feeder)

	type Session = com.excilys.ebi.gatling.core.session.Session
	type RequestStatus = com.excilys.ebi.gatling.core.result.message.RequestStatus
	type Simulation = com.excilys.ebi.gatling.core.scenario.configuration.Simulation
	type Feeder[T] = com.excilys.ebi.gatling.core.feeder.Feeder[T]
	type Assertion = com.excilys.ebi.gatling.core.structure.Assertion
	type Expression[T] = com.excilys.ebi.gatling.core.session.Expression[T]

	def scenario(scenarioName: String): ScenarioBuilder = ScenarioBuilder.scenario(scenarioName)
	val bootstrap = new ChainBuilder(Nil)

	val assertions = new AssertionBuilder

	implicit def string2path(string: String) = Path.string2path(string)

	val toInt = (x: String) => x.toInt
	val toLong = (x: String) => x.toInt
	val toFloat = (x: String) => x.toFloat
	val toDouble = (x: String) => x.toDouble
	val toBoolean = (x: String) => x.toBoolean

	/// Injection definitions

	private[Predef] class UserNumber(val number: Int) extends AnyVal
	private[Predef] class UsersPerSec(val rate: Double) extends AnyVal

	implicit class UserNumberImplicit(val number: Int) extends AnyVal {
		def user = users
		def users = new UserNumber(number)
	}
	implicit class UsersPerSecImplicit(val rate: Double) extends AnyVal {
		def userPerSec = usersPerSec
		def usersPerSec = new UsersPerSec(rate)
	}

	implicit def userNumber(number: Int) = new UserNumber(number)
	implicit def userPerSec(rate: Double) = new UsersPerSec(rate)

	case class RampBuilder(users: UserNumber) {
		def over(d: FiniteDuration) = new RampInjection(users.number, d)
	}
	case class ConstantRateBuilder(rate: UsersPerSec) {
		def during(d: FiniteDuration) = {
			val users = (d.toSeconds * rate.rate).toInt
			new RampInjection(users, d)
		}
	}

	def ramp(users: UserNumber) = new RampBuilder(users)
	def delay(d: FiniteDuration) = new DelayInjection(d)
	def peak(users: UserNumber) = new PeakInjection(users.number)
	def constantRate(rate: UsersPerSec) = new ConstantRateBuilder(rate)
}
