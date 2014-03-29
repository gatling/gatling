/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.check.ws

import io.gatling.core.check.{ CheckFactory, DefaultFindCheckBuilder }
import io.gatling.core.check.extractor.Extractor
import io.gatling.core.validation.SuccessWrapper
import io.gatling.core.session.ExpressionWrapper

object WebSocketPlainCheckBuilder {

  val extractor = new Extractor[String, String] {
    val name = "wsMessage"
    def apply(prepared: String) = Some(prepared).success
  }.expression

  def message(checkFactory: CheckFactory[WsCheck, String]) =
    new WebSocketPlainCheckBuilder(checkFactory)
}

class WebSocketPlainCheckBuilder(checkFactory: CheckFactory[WsCheck, String])
  extends DefaultFindCheckBuilder[WsCheck, String, String, String](
    checkFactory,
    WsCheckBuilders.passThroughMessagePreparer,
    WebSocketPlainCheckBuilder.extractor)
