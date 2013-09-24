/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core

import scala.concurrent.duration.DurationInt
import scala.reflect.ClassTag
import scala.reflect.io.Path

import io.gatling.core.assertion.AssertionSupport
import io.gatling.core.check.CheckSupport
import io.gatling.core.controller.inject.InjectionSupport
import io.gatling.core.controller.throttle.ThrottlingSupport
import io.gatling.core.feeder.FeederSupport
import io.gatling.core.pause.PauseSupport
import io.gatling.core.session.{ Expression, ExpressionWrapper }
import io.gatling.core.session.el.EL
import io.gatling.core.structure.{ ChainBuilder, ScenarioBuilder, StructureSupport }
import io.gatling.core.validation.{ SuccessWrapper, Validation }

object Predef extends StructureSupport with PauseSupport with CheckSupport with FeederSupport with InjectionSupport with ThrottlingSupport with AssertionSupport {

	type Session = io.gatling.core.session.Session
	type Status = io.gatling.core.result.message.Status
	type Simulation = io.gatling.core.scenario.Simulation
	type Assertion = io.gatling.core.assertion.Assertion

	implicit def stringToExpression[T: ClassTag](string: String): Expression[T] = string.el
	implicit def value2Success[T](value: T): Validation[T] = value.success
	implicit def value2Expression[T](value: T): Expression[T] = value.expression
	implicit def map2ExpressionMap(map: Map[String, Any]): Map[String, Expression[Any]] = map.mapValues(_ match {
		case string: String => string.el
		case any => any.expression
	})
	implicit def intToFiniteDuration(i: Int) = i seconds

	def scenario(scenarioName: String): ScenarioBuilder = ScenarioBuilder(scenarioName)

	implicit def string2path(string: String) = Path.string2path(string)
}
