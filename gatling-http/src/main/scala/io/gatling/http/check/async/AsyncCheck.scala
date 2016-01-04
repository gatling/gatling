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

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.util.TimeHelper._
import io.gatling.core.check.Check
import io.gatling.core.session.Session

sealed trait Expectation
case class UntilCount(count: Int) extends Expectation
case class ExpectedCount(count: Int) extends Expectation
case class ExpectedRange(range: Range) extends Expectation

case class AsyncCheck(
    wrapped:     Check[String],
    blocking:    Boolean,
    timeout:     FiniteDuration,
    expectation: Expectation,
    timestamp:   Long           = nowMillis
) extends Check[String] {
  override def check(message: String, session: Session)(implicit cache: mutable.Map[Any, Any]) =
    wrapped.check(message, session)
}
