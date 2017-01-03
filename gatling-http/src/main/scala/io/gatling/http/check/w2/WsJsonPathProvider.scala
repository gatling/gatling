/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
package io.gatling.http.check.w2

import io.gatling.core.check.{ CheckProtocolProvider, Preparer, Specializer }
import io.gatling.core.check.extractor.jsonpath.JsonPathCheckType
import io.gatling.core.json.JsonParsers
import io.gatling.http.action.ws2.WsTextCheck

class WsJsonPathProvider(jsonParsers: JsonParsers) extends CheckProtocolProvider[JsonPathCheckType, WsTextCheck, String, Any] {

  override val specializer: Specializer[WsTextCheck, String] = WsTextCheck(_)

  override val preparer: Preparer[String, Any] = jsonParsers.safeParseBoon
}
