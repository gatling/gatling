/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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

package io.gatling.http.check.async.message

import io.gatling.commons.validation._
import io.gatling.core.check.extractor._
import io.gatling.core.check.{ DefaultFindCheckBuilder, Extender }
import io.gatling.core.session._
import io.gatling.http.check.async.AsyncCheckBuilders._
import io.gatling.http.check.async.{ AsyncMessage, AsyncCheck }

object AsyncMessageStringCheckBuilder {

  val MessageStringExtractor = new Extractor[String, String] with SingleArity {
    val name = "messageString"
    def apply(prepared: String) = Some(prepared).success
  }.expressionSuccess

  def string(extender: Extender[AsyncCheck, AsyncMessage]) =
    new AsyncMessageStringCheckBuilder(extender)
}

class AsyncMessageStringCheckBuilder(extender: Extender[AsyncCheck, AsyncMessage])
  extends DefaultFindCheckBuilder[AsyncCheck, AsyncMessage, String, String](
    extender,
    AsyncMessageStringPreparer,
    AsyncMessageStringCheckBuilder.MessageStringExtractor
  )
