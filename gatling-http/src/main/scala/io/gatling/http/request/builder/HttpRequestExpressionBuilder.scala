/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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

import scala.collection.JavaConversions._

import io.gatling.commons.validation._
import io.gatling.core.body._
import io.gatling.core.session.Session
import io.gatling.http.ahc.AhcRequestBuilder
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.cache.ContentCacheEntry
import io.gatling.http.protocol.HttpComponents
import io.gatling.http.request.BodyPart

import org.asynchttpclient.uri.Uri
import org.asynchttpclient.request.body.generator.InputStreamBodyGenerator
import org.asynchttpclient.request.body.multipart.StringPart

class HttpRequestExpressionBuilder(commonAttributes: CommonAttributes, httpAttributes: HttpAttributes, httpComponents: HttpComponents)
    extends RequestExpressionBuilder(commonAttributes, httpComponents) {

  def configureCaches(session: Session)(requestBuilder: AhcRequestBuilder): AhcRequestBuilder = {

    httpCaches.contentCacheEntry(session, requestBuilder.underlyingRequest).foreach {
      case ContentCacheEntry(_, etag, lastModified) =>
        etag.foreach(requestBuilder.setHeader(HeaderNames.IfModifiedSince, _))
        lastModified.foreach(requestBuilder.setHeader(HeaderNames.IfNoneMatch, _))
    }

    requestBuilder
  }

  def configureFormParams(session: Session)(requestBuilder: AhcRequestBuilder): Validation[AhcRequestBuilder] =
    httpAttributes.formParams.mergeWithFormIntoParamJList(httpAttributes.form, session).map { resolvedFormParams =>
      if (httpAttributes.bodyParts.isEmpty) {
        // As a side effect, requestBuilder.setFormParams() resets the body data, so, it should not be called with empty parameters
        requestBuilder.setFormParams(resolvedFormParams)

      } else {
        resolvedFormParams.foreach(param => requestBuilder.addBodyPart(new StringPart(param.getName, param.getValue, null, charset)))
        requestBuilder
      }
    }

  def configureParts(session: Session)(requestBuilder: AhcRequestBuilder): Validation[AhcRequestBuilder] = {

    require(httpAttributes.body.isEmpty || httpAttributes.bodyParts.isEmpty, "Can't have both a body and body parts!")

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

      def setBodyParts(bodyParts: List[BodyPart]): Validation[AhcRequestBuilder] = {
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

  override protected def configureRequestBuilder(session: Session, uri: Uri, requestBuilder: AhcRequestBuilder): Validation[AhcRequestBuilder] =
    super.configureRequestBuilder(session, uri, requestBuilder)
      .map(configureCaches(session))
      .flatMap(configureFormParams(session))
      .flatMap(configureParts(session))
}
