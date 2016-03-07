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
package io.gatling.http.request.builder

import io.gatling.core.CoreComponents

import scala.collection.JavaConversions._

import io.gatling.commons.validation._
import io.gatling.core.body._
import io.gatling.core.session._
import io.gatling.http.ahc.AhcRequestBuilder
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.cache.ContentCacheEntry
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.BodyPart

import org.asynchttpclient.Request
import org.asynchttpclient.uri.Uri
import org.asynchttpclient.request.body.generator.InputStreamBodyGenerator
import org.asynchttpclient.request.body.multipart.StringPart

class HttpRequestExpressionBuilder(commonAttributes: CommonAttributes, httpAttributes: HttpAttributes, coreComponents: CoreComponents, httpComponents: HttpComponents)
    extends RequestExpressionBuilder(commonAttributes, coreComponents, httpComponents) {

  import RequestExpressionBuilder._

  private val configureFormParams0: RequestBuilderConfigure =
    session => requestBuilder => httpAttributes.formParams.mergeWithFormIntoParamJList(httpAttributes.form, session).map { resolvedFormParams =>
      if (httpAttributes.bodyParts.isEmpty) {
        // As a side effect, requestBuilder.setFormParams() resets the body data, so, it should not be called with empty parameters
        requestBuilder.setFormParams(resolvedFormParams)

      } else {
        resolvedFormParams.foreach(param => requestBuilder.addBodyPart(new StringPart(param.getName, param.getValue, null, charset)))
        requestBuilder
      }
    }

  private val configureFormParams: RequestBuilderConfigure =
    if (httpAttributes.formParams.isEmpty && httpAttributes.form.isEmpty)
      ConfigureIdentity
    else
      configureFormParams0

  private val configureParts0: RequestBuilderConfigure =
    session => requestBuilder => {

        def setBody(body: Body): Validation[AhcRequestBuilder] =
          body match {
            case StringBody(string) => string(session).map(requestBuilder.setBody)
            case RawFileBody(fileWithCachedBytes) => fileWithCachedBytes(session).map { f =>
              f.cachedBytes match {
                case Some(bytes) => requestBuilder.setBody(bytes)
                case None        => requestBuilder.setBody(f.file)
              }
            }
            case ByteArrayBody(bytes)          => bytes(session).map(requestBuilder.setBody)
            case CompositeByteArrayBody(bytes) => bytes(session).map(bs => requestBuilder.setBody(bs))
            case InputStreamBody(is)           => is(session).map(is => requestBuilder.setBody(new InputStreamBodyGenerator(is)))
          }

        def setBodyParts(bodyParts: List[BodyPart]): Validation[AhcRequestBuilder] =
          bodyParts.foldLeft(requestBuilder.success) { (requestBuilder, part) =>
            for {
              requestBuilder <- requestBuilder
              part <- part.toMultiPart(session)
            } yield requestBuilder.addBodyPart(part)
          }

      httpAttributes.body match {
        case None       => setBodyParts(httpAttributes.bodyParts)
        case Some(body) => setBody(body)
      }
    }

  private val configureParts: RequestBuilderConfigure = {
    require(httpAttributes.body.isEmpty || httpAttributes.bodyParts.isEmpty, "Can't have both a body and body parts!")

    if (httpAttributes.body.isEmpty && httpAttributes.bodyParts.isEmpty)
      ConfigureIdentity
    else
      configureParts0
  }

  override protected def addDefaultHeaders(session: Session)(requestBuilder: AhcRequestBuilder): AhcRequestBuilder = {
    super.addDefaultHeaders(session)(requestBuilder)
    if (contentTypeHeaderIsUndefined) {
      if (httpAttributes.bodyParts.nonEmpty)
        requestBuilder.addHeader(HeaderNames.ContentType, HeaderValues.MultipartFormData)
      else if (httpAttributes.formParams.nonEmpty)
        requestBuilder.addHeader(HeaderNames.ContentType, HeaderValues.ApplicationFormUrlEncoded)
    }
    requestBuilder
  }

  override protected def configureRequestBuilder(session: Session, uri: Uri, requestBuilder: AhcRequestBuilder): Validation[AhcRequestBuilder] =
    super.configureRequestBuilder(session, uri, requestBuilder)
      .flatMap(configureFormParams(session))
      .flatMap(configureParts(session))

  private def configureCachingHeaders(session: Session)(request: Request): Request = {

    httpCaches.contentCacheEntry(session, request).foreach {
      case ContentCacheEntry(_, etag, lastModified) =>
        etag.foreach(request.getHeaders.set(HeaderNames.IfNoneMatch, _))
        lastModified.foreach(request.getHeaders.set(HeaderNames.IfModifiedSince, _))
    }

    request
  }

  // hack because we need the request with the final uri
  override def build: Expression[Request] = {
    val exp = super.build
    if (httpComponents.httpProtocol.requestPart.cache) {
      session => exp(session).map(configureCachingHeaders(session))
    } else {
      exp
    }
  }
}
