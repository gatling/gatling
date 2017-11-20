/*
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

import io.gatling.core.check._
import io.gatling.core.json.JsonParsers
import io.gatling.http.action.ws2.WsTextCheck

trait WsCheckSupport {

  implicit def checkBuilder2WsCheck[A, P, X](checkBuilder: CheckBuilder[A, P, X])(implicit provider: CheckProtocolProvider[A, WsTextCheck, String, P]): WsTextCheck =
    checkBuilder.build(provider)

  implicit def validatorCheckBuilder2WsCheck[A, P, X](validatorCheckBuilder: ValidatorCheckBuilder[A, P, X])(implicit provider: CheckProtocolProvider[A, WsTextCheck, String, P]): WsTextCheck =
    validatorCheckBuilder.exists

  implicit def findCheckBuilder2WsCheck[A, P, X](findCheckBuilder: FindCheckBuilder[A, P, X])(implicit provider: CheckProtocolProvider[A, WsTextCheck, String, P]): WsTextCheck =
    findCheckBuilder.find.exists

  implicit def wsJsonPathProvider(implicit jsonParsers: JsonParsers) = new WsJsonPathProvider(jsonParsers)

  implicit val wsRegexProvider = WsRegexProvider
}
