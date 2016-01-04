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

import io.gatling.commons.validation._
import io.gatling.core.check.{ Extender, DefaultFindCheckBuilder }
import io.gatling.core.check.extractor._
import io.gatling.core.session._
import io.gatling.http.check.async.AsyncCheckBuilders._

object AsyncPlainCheckBuilder {

  val WsPlainExtractor = new Extractor[String, String] with SingleArity {
    val name = "wsMessage"
    def apply(prepared: String) = Some(prepared).success
  }.expressionSuccess

  def message(extender: Extender[AsyncCheck, String]) =
    new AsyncPlainCheckBuilder(extender)
}

class AsyncPlainCheckBuilder(extender: Extender[AsyncCheck, String])
  extends DefaultFindCheckBuilder[AsyncCheck, String, String, String](
    extender,
    PassThroughMessagePreparer,
    AsyncPlainCheckBuilder.WsPlainExtractor
  )
