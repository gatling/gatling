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
package com.excilys.ebi.gatling.http.cache

import org.specs2.mock._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import com.excilys.ebi.gatling.http.check.body.HttpBodyRegexCheckBuilder
import com.excilys.ebi.gatling.core.session.Session
import scalaz._
import Scalaz._
import com.excilys.ebi.gatling.core.session.Expression
import com.excilys.ebi.gatling.http.response.ExtendedResponse
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.ning.http.client.Response
import com.excilys.ebi.gatling.core.config.GatlingPropertiesBuilder
import com.excilys.ebi.gatling.core.config.GatlingConfiguration
import com.excilys.ebi.gatling.core.check.CheckContext.useCheckContext

@RunWith(classOf[JUnitRunner])
class HttpBodyRegexCheckBuilderSpec extends Specification {

	implicit def stringToStringExpression(string: String) = Expression[String](string)
	implicit def value2Success[T](value: T): Validation[String, T] = value.success
	implicit def value2Expression[T](value: T): Expression[T] = (session: Session) => value.success

	val init = {
		val props = new GatlingPropertiesBuilder
		props.sourcesDirectory("src/test/resources")
		props.resultsDirectory("src/test/resources")

		GatlingConfiguration.setUp(props.build)
	}

	"HttpBodyRegexCheckBuilder" should {
		"produce a correct answer for a basic check" in {

			val session = new Session("scenario", 1, Map())

			val regexCheck = HttpBodyRegexCheckBuilder.regex("^([a-z]+)").find.is("abc").build

			case class GMock() extends Mockito {
				def response() = {
					val er = smartMock[ExtendedResponse]
					er.getResponseBody(configuration.simulation.encoding) returns "abcDEF"
					er
				}
			}
			
			useCheckContext {
				val b = regexCheck(GMock().response())(session)
				b.isSuccess must beTrue
			}
		}
	}
}