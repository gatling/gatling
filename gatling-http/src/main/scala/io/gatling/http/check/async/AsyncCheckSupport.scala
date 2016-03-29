/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.check.async

import io.gatling.core.check.extractor.jsonpath.JsonPathExtractorFactory
import io.gatling.core.check.extractor.regex.RegexExtractorFactory
import io.gatling.core.json.JsonParsers

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.session.Expression

trait AsyncCheckSupport extends AsyncCheckDSL {

  implicit def checkTypeStep2Check(step: CheckTypeStep): AsyncCheckBuilder = step.message.find.exists
}

trait AsyncCheckDSL {

  // TODO: rename those !
  val wsListen = new TimeoutStep(false)
  val wsAwait = new TimeoutStep(true)

  class TimeoutStep(await: Boolean) {
    def within(timeout: FiniteDuration) = new ExpectationStep(await, timeout)
  }

  class ExpectationStep(await: Boolean, timeout: FiniteDuration) {
    def until(count: Int) = new CheckTypeStep(await, timeout, UntilCount(count))
    def expect(count: Int) = new CheckTypeStep(await, timeout, ExpectedCount(count))
    def expect(range: Range) = new CheckTypeStep(await, timeout, ExpectedRange(range))
  }

  class CheckTypeStep(await: Boolean, timeout: FiniteDuration, expectation: Expectation) {

    def regex(expression: Expression[String])(implicit extractorFactory: RegexExtractorFactory) =
      AsyncRegexCheckBuilder.regex(expression, AsyncCheckBuilders.extender(await, timeout, expectation))

    def jsonPath(path: Expression[String])(implicit extractorFactory: JsonPathExtractorFactory, jsonParsers: JsonParsers) =
      AsyncJsonPathCheckBuilder.jsonPath(path, AsyncCheckBuilders.extender(await, timeout, expectation))

    def jsonpJsonPath(path: Expression[String])(implicit extractorFactory: JsonPathExtractorFactory, jsonParsers: JsonParsers) =
      AsyncJsonpJsonPathCheckBuilder.jsonpJsonPath(path, AsyncCheckBuilders.extender(await, timeout, expectation))

    val message = AsyncPlainCheckBuilder.message(AsyncCheckBuilders.extender(await, timeout, expectation))
  }
}
