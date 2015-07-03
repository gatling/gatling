/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.builder.{ RequestExpressionBuilder, CommonAttributes }
import io.gatling.http.util.HttpHelper

import org.asynchttpclient.uri.Uri

class WsRequestExpressionBuilder(commonAttributes: CommonAttributes, httpComponents: HttpComponents)
    extends RequestExpressionBuilder(commonAttributes, httpComponents) {

  def makeAbsolute(url: String): Validation[Uri] =
    if (HttpHelper.isAbsoluteWsUrl(url))
      Uri.create(url).success
    else
      protocol.wsPart.wsBaseURL match {
        case Some(baseURL) => Uri.create(baseURL, url).success
        case _             => s"No protocol.baseURL defined but provided url is relative : $url".failure
      }
}
