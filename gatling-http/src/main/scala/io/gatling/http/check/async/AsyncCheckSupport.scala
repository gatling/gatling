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
  val wsListen = new TimeoutStep()
  val wsAwait = new AwaitTimeoutStep()

  class TimeoutStep() {
    def within(timeout: FiniteDuration) = new ExpectationStep(timeout)
  }

  class AwaitTimeoutStep() {
    def within(timeout: FiniteDuration) = new AwaitExpectationStep(timeout)
  }

  class AwaitExpectationStep(timeout: FiniteDuration) {
    def until(count: Int) = new CheckTypeStep(true, timeout, UntilCount(count))
    def expect(count: Int) = new CheckTypeStep(true, timeout, ExpectedCount(count))
    def expect(range: Range) = new CheckTypeStep(true, timeout, ExpectedRange(range))
  }

  class ExpectationStep(timeout: FiniteDuration) {
    def until(count: Int) = new CheckTypeStep(false, timeout, UntilCount(count))
    def expect(count: Int) = new CheckTypeStep(false, timeout, ExpectedCount(count))
    def expect(range: Range) = new CheckTypeStep(false, timeout, ExpectedRange(range))

    /**
     * Checks of this type will all be kept until they're matched by any async server message, or they timeout
     *
     * until/expect can't be used: they're necessarily checked until they're ok once
     *
     * can only be called on 'wsListen'/non blocking step
     *
     * Implemented ONLY on Websocket - NOT on SSE
     */
    def accumulate = new CheckTypeStep(false, timeout, UntilCount(1), true)
  }

  class CheckTypeStep(await: Boolean, timeout: FiniteDuration, expectation: Expectation, shouldAccumulate: Boolean = false) {

    def regex(expression: Expression[String])(implicit extractorFactory: RegexExtractorFactory) =
      AsyncRegexCheckBuilder.regex(expression, AsyncCheckBuilders.extender(await, timeout, expectation, shouldAccumulate))

    def jsonPath(path: Expression[String])(implicit extractorFactory: JsonPathExtractorFactory, jsonParsers: JsonParsers) =
      AsyncJsonPathCheckBuilder.jsonPath(path, AsyncCheckBuilders.extender(await, timeout, expectation, shouldAccumulate))

    def jsonpJsonPath(path: Expression[String])(implicit extractorFactory: JsonPathExtractorFactory, jsonParsers: JsonParsers) =
      AsyncJsonpJsonPathCheckBuilder.jsonpJsonPath(path, AsyncCheckBuilders.extender(await, timeout, expectation, shouldAccumulate))

    val message = AsyncPlainCheckBuilder.message(AsyncCheckBuilders.extender(await, timeout, expectation, shouldAccumulate))
  }
}
