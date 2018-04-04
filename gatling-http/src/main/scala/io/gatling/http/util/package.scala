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

package io.gatling.http

import java.lang.{ StringBuilder => JStringBuilder }
import java.nio.charset.Charset
import java.util.{ List => JList }

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import io.gatling.commons.util.StringHelper.Eol
import io.gatling.commons.util.Throwables._
import io.gatling.http.client.body._
import io.gatling.http.client.body.part.{ ByteArrayPart, FilePart, StringPart }
import io.gatling.http.client.{ Param, Request }
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper.isTxt

import com.typesafe.scalalogging.LazyLogging
import io.netty.handler.codec.http.HttpHeaders

package object util extends LazyLogging {

  implicit class HttpStringBuilder(val buff: JStringBuilder) extends AnyVal {

    def appendHttpHeaders(headers: HttpHeaders): JStringBuilder = {
      headers.asScala.foreach { entry =>
        buff.append(entry.getKey).append(": ").append(entry.getValue).append(Eol)
      }
      buff
    }

    def appendParamJList(list: JList[Param]): JStringBuilder = {
      list.asScala.foreach { param =>
        buff.append(param.getName).append(": ").append(param.getValue).append(Eol)
      }
      buff
    }

    def appendRequest(request: Request, wireRequestHeaders: Option[HttpHeaders], charset: Charset): JStringBuilder = {

      buff.append(request.getMethod).append(" ").append(request.getUri.toUrl).append(Eol)

      wireRequestHeaders match {
        case Some(headers) =>

          if (!headers.isEmpty) {
            buff.append("headers=").append(Eol)
            for (header <- headers.asScala) {
              buff.append(header.getKey).append(": ").append(header.getValue).append(Eol)
            }
          }

        case _ =>
          if (!request.getHeaders.isEmpty) {
            buff.append("headers=").append(Eol)
            buff.appendHttpHeaders(request.getHeaders)
          }

          if (!request.getCookies.isEmpty) {
            buff.append("cookies=").append(Eol)
            for (cookie <- request.getCookies.asScala) {
              buff.append(cookie).append(Eol)
            }
          }
      }

      request.getBody match {
        case stringBody: StringRequestBody =>
          buff.append("stringBody=").append(stringBody.getContent).append(Eol)

        case byteArrayBody: ByteArrayRequestBody =>
          buff.append("byteBody=").append(new String(byteArrayBody.getContent, charset)).append(Eol)

        case byteArraysBody: ByteArraysRequestBody =>
          buff.append("byteArraysBody=")
          byteArraysBody.getContent.foreach(b => buff.append(new String(b, charset)))
          buff.append(Eol)

        case fileBody: FileRequestBody =>
          buff.append("fileBody=").append(fileBody.getContent.getCanonicalPath).append(Eol)

        case formBody: FormUrlEncodedRequestBody =>
          buff.append("formBody=").append(Eol).appendParamJList(formBody.getContent)

        case streamBody: InputStreamRequestBody =>
          buff.append("streamBody=")

        case multipartBody: MultipartFormDataRequestBody =>
          buff.append("multipartBody=").append(Eol)
          multipartBody.getContent.asScala.foreach {
            case part: StringPart =>
              buff
                .append("StringPart:")
                .append(" name=").append(part.getName)
                .append(" contentType=").append(part.getContentType)
                .append(" dispositionType=").append(part.getDispositionType)
                .append(" charset=").append(part.getCharset)
                .append(" transferEncoding=").append(part.getTransferEncoding)
                .append(" contentId=").append(part.getContentId)
                .append(Eol)

            case part: FilePart =>
              buff.append("FilePart:")
                .append(" name=").append(part.getName)
                .append(" contentType=").append(part.getContentType)
                .append(" dispositionType=").append(part.getDispositionType)
                .append(" charset=").append(part.getCharset)
                .append(" transferEncoding=").append(part.getTransferEncoding)
                .append(" contentId=").append(part.getContentId)
                .append(" filename=").append(part.getFileName)
                .append(" file=").append(part.getContent.getCanonicalPath)
                .append(Eol)

            case part: ByteArrayPart =>
              buff.append("ByteArrayPart:")
                .append(" name=").append(part.getName)
                .append(" contentType=").append(part.getContentType)
                .append(" dispositionType=").append(part.getDispositionType)
                .append(" charset=").append(part.getCharset)
                .append(" transferEncoding=").append(part.getTransferEncoding)
                .append(" contentId=").append(part.getContentId)
                .append(" filename=").append(part.getFileName)
                .append(Eol)
          }

      }

      if (request.getProxyServer != null) buff.append("proxy=").append(request.getProxyServer).append(Eol)

      if (request.getRealm != null) buff.append("realm=").append(request.getRealm).append(Eol)

      buff
    }

    def appendResponse(response: Response): JStringBuilder = {

      response.status.foreach { status =>
        buff.append("status=").append(Eol).append(status).append(Eol)

        if (!response.headers.isEmpty) {
          buff.append("headers= ").append(Eol)
          buff.appendHttpHeaders(response.headers).append(Eol)
        }

        if (response.hasResponseBody) {
          buff.append("body=").append(Eol)
          if (isTxt(response.headers)) {
            try {
              buff.append(response.body.string)
            } catch {
              case NonFatal(t) =>
                val message = "Could not decode response body"
                logger.trace(message, t)
                buff.append(s"$message: ${t.rootMessage}")
            }
          } else {
            buff.append("<<<BINARY CONTENT>>>")
          }
        }
      }

      buff
    }
  }
}
