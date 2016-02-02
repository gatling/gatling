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
package io.gatling.http.request.builder.ws

import io.gatling.commons.validation.Validation
import io.gatling.core.CoreComponents
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.builder.{ RequestExpressionBuilder, CommonAttributes }

import org.asynchttpclient.uri.Uri

class WsRequestExpressionBuilder(commonAttributes: CommonAttributes, coreComponents: CoreComponents, httpComponents: HttpComponents)
    extends RequestExpressionBuilder(commonAttributes, coreComponents, httpComponents) {

  override def makeAbsolute(url: String): Validation[Uri] =
    protocol.wsPart.makeAbsoluteWsUri(url)
}
