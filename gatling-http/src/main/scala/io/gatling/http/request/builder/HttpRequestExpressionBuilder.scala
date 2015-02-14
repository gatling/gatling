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

import scala.collection.JavaConversions._

import com.ning.http.client.multipart.StringPart
import com.ning.http.client.uri.Uri
import com.ning.http.client.{ RequestBuilder => AHCRequestBuilder }
import com.ning.http.client.generators.InputStreamBodyGenerator

import io.gatling.core.body._
import io.gatling.core.config.GatlingConfiguration._
import io.gatling.core.session.Session
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.cache.CacheHandling
import io.gatling.http.config.HttpProtocol
import io.gatling.http.request.BodyPart
import io.gatling.http.util.HttpHelper

class HttpRequestExpressionBuilder(commonAttributes: CommonAttributes, httpAttributes: HttpAttributes, protocol: HttpProtocol)
    extends RequestExpressionBuilder(commonAttributes, protocol) {

  def makeAbsolute(url: String): Validation[String] =
    if (HttpHelper.isAbsoluteHttpUrl(url))
      url.success
    else
      protocol.baseURL match {
        case Some(baseURL) => (baseURL + url).success
        case _             => s"No protocol.baseURL defined but provided url is relative : $url".failure
      }

  def configureCaches(session: Session, uri: Uri)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] = {
    CacheHandling.getLastModified(session, uri, commonAttributes.method).foreach(requestBuilder.setHeader(HeaderNames.IfModifiedSince, _))
    CacheHandling.getEtag(session, uri, commonAttributes.method).foreach(requestBuilder.setHeader(HeaderNames.IfNoneMatch, _))
    requestBuilder.success
  }

  def configureFormParams(session: Session)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] = {

      def configureFormParamsAsParams: Validation[AHCRequestBuilder] = httpAttributes.formParams match {
        case Nil => requestBuilder.success
        case params =>
          // As a side effect, requestBuilder.setFormParams() resets the body data, so, it should not be called with empty parameters
          params.resolveParamJList(session).map(requestBuilder.setFormParams)
      }

      def configureFormParamsAsStringParts: Validation[AHCRequestBuilder] =
        httpAttributes.formParams.resolveParamJList(session).map { params =>
          for {
            param <- params
          } requestBuilder.addBodyPart(new StringPart(param.getName, param.getValue, null, configuration.core.charset))

          requestBuilder
        }

    httpAttributes.bodyParts match {
      case Nil => configureFormParamsAsParams
      case _   => configureFormParamsAsStringParts
    }
  }

  def configureParts(session: Session)(requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] = {

    require(!httpAttributes.body.isDefined || httpAttributes.bodyParts.isEmpty, "Can't have both a body and body parts!")

      def setBody(body: Body): Validation[AHCRequestBuilder] =
        body match {
          case StringBody(string)            => string(session).map(requestBuilder.setBody)
          case RawFileBody(file)             => file(session).map(requestBuilder.setBody)
          case ByteArrayBody(bytes)          => bytes(session).map(requestBuilder.setBody)
          case CompositeByteArrayBody(bytes) => bytes(session).map(bs => requestBuilder.setBody(bs))
          case InputStreamBody(is)           => is(session).map(is => requestBuilder.setBody(new InputStreamBodyGenerator(is)))
        }

      def setBodyParts(bodyParts: List[BodyPart]): Validation[AHCRequestBuilder] = {
        if (!commonAttributes.headers.contains(HeaderNames.ContentType))
          requestBuilder.addHeader(HeaderNames.ContentType, HeaderValues.MultipartFormData)

        bodyParts.foldLeft(requestBuilder.success) { (requestBuilder, part) =>
          for {
            requestBuilder <- requestBuilder
            part <- part.toMultiPart(session)
          } yield requestBuilder.addBodyPart(part)
        }
      }

    httpAttributes.body match {
      case Some(body) => setBody(body)

      case None => httpAttributes.bodyParts match {
        case Nil       => requestBuilder.success
        case bodyParts => setBodyParts(bodyParts)
      }
    }
  }

  override protected def configureRequestBuilder(session: Session, uri: Uri, requestBuilder: AHCRequestBuilder): Validation[AHCRequestBuilder] =
    super.configureRequestBuilder(session, uri, requestBuilder)
      .flatMap(configureCaches(session, uri))
      .flatMap(configureFormParams(session))
      .flatMap(configureParts(session))
}
