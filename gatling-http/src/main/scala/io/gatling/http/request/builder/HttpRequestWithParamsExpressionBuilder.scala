/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request.builder

import com.ning.http.client.{ RequestBuilder => AHCRequestBuilder }
import com.ning.http.multipart.StringPart

import io.gatling.core.session.Session
import io.gatling.core.validation.{ SuccessWrapper, Validation }
import io.gatling.http.config.HttpProtocol

class HttpRequestWithParamsExpressionBuilder(commonAttributes: CommonAttributes, httpAttributes: HttpAttributes, params: List[HttpParam], protocol: HttpProtocol)
    extends HttpRequestExpressionBuilder(commonAttributes, httpAttributes, protocol) {

  override def configureParts(session: Session)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] = {

      def configureAsParams: Validation[AHCRequestBuilder] = params match {
        case Nil => requestBuilder.success
        case _ =>
          // As a side effect, requestBuilder.setParameters() resets the body data, so, it should not be called with empty parameters 
          params.resolveFluentStringsMap(session).map(requestBuilder.setParameters)
      }

      def configureAsStringParts: Validation[AHCRequestBuilder] =
        params.resolveParams(session).map { params =>
          for {
            (key, values) <- params
            value <- values
          } requestBuilder.addBodyPart(new StringPart(key, value))

          requestBuilder
        }

    val requestBuilderWithParams = httpAttributes.bodyParts match {
      case Nil => configureAsParams
      case _   => configureAsStringParts
    }

    requestBuilderWithParams.flatMap(super.configureParts(session))
  }
}
