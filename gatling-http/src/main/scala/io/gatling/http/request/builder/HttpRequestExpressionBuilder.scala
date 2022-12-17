/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import java.{ util => ju }

import scala.jdk.CollectionConverters._

import io.gatling.commons.validation._
import io.gatling.core.body._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.cache.{ ContentCacheEntry, Http2PriorKnowledgeSupport, HttpCaches }
import io.gatling.http.client.{ Param, Request, RequestBuilder => ClientRequestBuilder }
import io.gatling.http.client.body.RequestBodyBuilder
import io.gatling.http.client.body.bytearray.ByteArrayRequestBodyBuilder
import io.gatling.http.client.body.file.FileRequestBodyBuilder
import io.gatling.http.client.body.form.FormUrlEncodedRequestBodyBuilder
import io.gatling.http.client.body.is.InputStreamRequestBodyBuilder
import io.gatling.http.client.body.multipart.{ MultipartFormDataRequestBodyBuilder, Part, StringPart }
import io.gatling.http.client.body.string.StringRequestBodyBuilder
import io.gatling.http.client.body.stringchunks.StringChunksRequestBodyBuilder
import io.gatling.http.protocol.{ HttpProtocol, Remote }
import io.gatling.http.request.BodyPart
import io.gatling.http.util.HttpHelper

import io.netty.handler.codec.http.HttpHeaderNames

object HttpRequestExpressionBuilder {
  private val bodyPartsToMultipartsZero = List.empty[Part[_]].success

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  private def bodyPartsToMultiparts(bodyParts: List[BodyPart], session: Session): Validation[List[Part[_]]] =
    bodyParts.foldLeft(bodyPartsToMultipartsZero) { (acc, bodyPart) =>
      for {
        accValue <- acc
        value <- bodyPart.toMultiPart(session)
      } yield accValue :+ value
    }
}

class HttpRequestExpressionBuilder(
    commonAttributes: CommonAttributes,
    httpAttributes: HttpAttributes,
    httpCaches: HttpCaches,
    httpProtocol: HttpProtocol,
    configuration: GatlingConfiguration
) extends RequestExpressionBuilder(commonAttributes, httpCaches, httpProtocol, configuration) {

  private def mergeFormParamsAndFormIntoParamJList(
      params: List[HttpParam],
      formMaybe: Option[Expression[Map[String, Any]]],
      session: Session
  ): Validation[ju.List[Param]] = {
    val formParams = resolveParamJList(params, session)

    formMaybe match {
      case Some(form) =>
        for {
          resolvedFormParams <- formParams
          resolvedForm <- form(session)
        } yield {
          val formParamsByName = resolvedFormParams.asScala.groupBy(_.getName)
          val formFieldsByName: Map[String, Seq[Param]] =
            resolvedForm.map { case (key, value) =>
              value match {
                case multipleValues: Seq[_] => key -> multipleValues.map(value => new Param(key, value.toString))
                case monoValue              => key -> Seq(new Param(key, monoValue.toString))
              }
            }
          // override form with formParams
          val javaParams: ju.List[Param] = (formFieldsByName ++ formParamsByName).values.flatten.toSeq.asJava
          javaParams
        }

      case _ =>
        formParams
    }
  }

  private def configureMultipartFormData(session: Session, requestBuilder: ClientRequestBuilder): Validation[_] =
    for {
      params <- mergeFormParamsAndFormIntoParamJList(httpAttributes.formParams, httpAttributes.form, session)
      stringParts = params.asScala.map(param => new StringPart(param.getName, param.getValue, charset, null, null, null, null, null))
      parts <- HttpRequestExpressionBuilder.bodyPartsToMultiparts(httpAttributes.bodyParts, session)
    } yield requestBuilder.setBodyBuilder(new MultipartFormDataRequestBodyBuilder((stringParts ++ parts).asJava))

  private def configureFormUrlEncoded(session: Session, requestBuilder: ClientRequestBuilder): Validation[_] =
    for {
      params <- mergeFormParamsAndFormIntoParamJList(httpAttributes.formParams, httpAttributes.form, session)
    } yield requestBuilder.setBodyBuilder(new FormUrlEncodedRequestBodyBuilder(params))

  private val maybeRequestBodyBuilderExpression: Option[Expression[RequestBodyBuilder]] =
    httpAttributes.body.map {
      case StringBody(string, _) => string(_).map(new StringRequestBodyBuilder(_))
      case RawFileBody(resourceWithCachedBytes) =>
        resourceWithCachedBytes(_).map { case ResourceAndCachedBytes(resource, cachedBytes) =>
          cachedBytes match {
            case Some(bytes) => new ByteArrayRequestBodyBuilder(bytes, resource.name)
            case _           => new FileRequestBodyBuilder(resource.file)
          }
        }
      case ByteArrayBody(bytes) => bytes(_).map(new ByteArrayRequestBodyBuilder(_, null))
      case body: ElBody         => body.asStringWithCachedBytes(_).map(chunks => new StringChunksRequestBodyBuilder(chunks.asJava))
      case InputStreamBody(is)  => is(_).map(new InputStreamRequestBodyBuilder(_))
    }

  private val hasParts = httpAttributes.bodyParts.nonEmpty
  private val hasForm = httpAttributes.formParams.nonEmpty || httpAttributes.form.nonEmpty
  private def configureBody(session: Session, requestBuilder: ClientRequestBuilder): Validation[_] = {
    require(httpAttributes.body.isEmpty || httpAttributes.bodyParts.isEmpty, "Can't have both a body and body parts!")

    maybeRequestBodyBuilderExpression match {
      case Some(requestBodyBuilderExpression) =>
        requestBodyBuilderExpression(session).map(requestBuilder.setBodyBuilder)
      case _ =>
        if (hasParts || (hasForm && HttpHelper.isMultipartFormData(requestBuilder.getContentType))) {
          configureMultipartFormData(session, requestBuilder)
        } else if (hasForm) {
          configureFormUrlEncoded(session, requestBuilder)
        } else {
          Validation.unit
        }
    }
  }

  private val enableHttp2 = httpProtocol.enginePart.enableHttp2
  private def configurePriorKnowledge(session: Session, requestBuilder: ClientRequestBuilder): Unit =
    if (enableHttp2) {
      val http2PriorKnowledge = Http2PriorKnowledgeSupport.isHttp2PriorKnowledge(session, Remote(requestBuilder.getUri))
      requestBuilder
        .setHttp2Enabled(true)
        .setAlpnRequired(http2PriorKnowledge.forall(_ == true)) // ALPN is necessary only if we know that this remote is using HTTP/2 or if we still don't know
        .setHttp2PriorKnowledge(http2PriorKnowledge.contains(true))
    }

  private val requestTimeout = httpAttributes.requestTimeout.getOrElse(configuration.http.requestTimeout).toMillis
  override protected def configureRequestTimeout(requestBuilder: ClientRequestBuilder): Unit =
    requestBuilder.setRequestTimeout(requestTimeout)

  override protected def configureRequestBuilderForProtocol(session: Session, requestBuilder: ClientRequestBuilder): Validation[_] =
    for {
      _ <- configureBody(session, requestBuilder)
    } yield configurePriorKnowledge(session, requestBuilder)

  private def configureCachingHeaders(session: Session)(request: Request): Request = {
    httpCaches.contentCacheEntry(session, request).foreach { case ContentCacheEntry(_, etag, lastModified) =>
      etag.foreach(request.getHeaders.set(HttpHeaderNames.IF_NONE_MATCH, _))
      lastModified.foreach(request.getHeaders.set(HttpHeaderNames.IF_MODIFIED_SINCE, _))
    }
    request
  }

  // hack because we need the request with the final uri
  override def build: Expression[Request] = {
    val exp = super.build
    if (httpProtocol.requestPart.cache) { session =>
      exp(session).map(configureCachingHeaders(session))
    } else {
      exp
    }
  }
}
