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
package io.gatling.http.request.builder.sse

import io.gatling.core.session.Session
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.builder.{ CommonAttributes, RequestExpressionBuilder }
import io.gatling.http.util.HttpHelper

import org.asynchttpclient.{ RequestBuilder => AHCRequestBuilder }
import org.asynchttpclient.uri.Uri

class SseRequestExpressionBuilder(commonAttributes: CommonAttributes, httpComponents: HttpComponents)
    extends RequestExpressionBuilder(commonAttributes, httpComponents) {

  override protected def configureRequestBuilder(session: Session, uri: Uri, requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] = {
    // disable request timeout for SSE
    requestBuilder.setRequestTimeout(-1)
    super.configureRequestBuilder(session, uri, requestBuilder)
  }

  def makeAbsolute(url: String): Validation[Uri] =
    if (HttpHelper.isAbsoluteHttpUrl(url))
      Uri.create(url).success
    else
      protocol.baseURL match {
        case Some(baseURL) => Uri.create(baseURL, url).success
        case _             => s"No protocol.baseURL defined but provided url is relative : $url".failure
      }
}
