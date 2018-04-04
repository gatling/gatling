/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import scala.collection.JavaConverters._

import io.gatling.commons.validation._
import io.gatling.core.CoreComponents
import io.gatling.core.body._
import io.gatling.core.session._
import io.gatling.core.util.FileResource
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.cache.ContentCacheEntry
import io.gatling.http.client.body._
import io.gatling.http.client.body.part.StringPart
import io.gatling.http.client.{ Request, RequestBuilder => AhcRequestBuilder }
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.BodyPart

class HttpRequestExpressionBuilder(commonAttributes: CommonAttributes, httpAttributes: HttpAttributes, coreComponents: CoreComponents, httpComponents: HttpComponents)
  extends RequestExpressionBuilder(commonAttributes, coreComponents, httpComponents) {

  import RequestExpressionBuilder._

  private val ConfigureFormParams: RequestBuilderConfigure =
    session => requestBuilder => httpAttributes.formParams.mergeWithFormIntoParamJList(httpAttributes.form, session).map { resolvedFormParams =>
      requestBuilder.setBody(new FormUrlEncodedRequestBody(resolvedFormParams))
    }

  private def configureBodyParts(session: Session, requestBuilder: AhcRequestBuilder, bodyParts: List[BodyPart]): Validation[AhcRequestBuilder] =
    for {
      params <- httpAttributes.formParams.mergeWithFormIntoParamJList(httpAttributes.form, session)
      stringParts = params.asScala.map(param => new StringPart(param.getName, param.getValue, charset, null, null, null, null))
      parts <- Validation.sequence(bodyParts.map(_.toMultiPart(session)))
    } yield requestBuilder.setBody(new MultipartFormDataRequestBody((parts ++ stringParts).asJava))

  private def setBody(session: Session, requestBuilder: AhcRequestBuilder, body: Body): Validation[AhcRequestBuilder] =
    body match {
      case StringBody(string) => string(session).map(s => requestBuilder.setBody(new StringRequestBody(s)))
      case RawFileBody(resourceWithCachedBytes) => resourceWithCachedBytes(session).map {
        case ResourceAndCachedBytes(resource, cachedBytes) =>
          cachedBytes match {
            case Some(bytes) => requestBuilder.setBody(new ByteArrayRequestBody(bytes))
            case None =>
              resource match {
                case FileResource(file) => requestBuilder.setBody(new FileRequestBody(file))
                case _                  => requestBuilder.setBody(new ByteArrayRequestBody(resource.bytes))
              }
          }
      }
      case ByteArrayBody(bytes)                  => bytes(session).map(b => requestBuilder.setBody(new ByteArrayRequestBody(b)))
      case CompositeByteArrayBody(byteArrays, _) => byteArrays(session).map(bs => requestBuilder.setBody(new ByteArraysRequestBody(bs.toArray)))
      case InputStreamBody(is)                   => is(session).map(is => requestBuilder.setBody(new InputStreamRequestBody(is)))
      case body: PebbleBody                      => body.apply(session).map(s => requestBuilder.setBody(new StringRequestBody(s)))
    }

  private val configureBody: RequestBuilderConfigure = {
    require(httpAttributes.body.isEmpty || httpAttributes.bodyParts.isEmpty, "Can't have both a body and body parts!")

    httpAttributes.body match {
      case Some(body) => session => requestBuilder => setBody(session, requestBuilder, body)
      case _ =>
        if (httpAttributes.bodyParts.nonEmpty) { session => requestBuilder => configureBodyParts(session, requestBuilder, httpAttributes.bodyParts)
        } else if (httpAttributes.formParams.nonEmpty || httpAttributes.form.nonEmpty) {
          ConfigureFormParams
        } else {
          ConfigureIdentity
        }
    }
  }

  override protected def configureRequestBuilder(session: Session, requestBuilder: AhcRequestBuilder): Validation[AhcRequestBuilder] =
    super.configureRequestBuilder(session, requestBuilder)
      .flatMap(configureBody(session))

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
