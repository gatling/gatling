/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

import java.{ lang => jl }
import java.nio.charset.Charset

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import io.gatling.commons.util.StringHelper.Eol
import io.gatling.commons.util.Throwables._
import io.gatling.http.response.{ HttpResult, Response }
import io.gatling.http.util.HttpHelper.isText

import com.typesafe.scalalogging.LazyLogging
import io.netty.handler.codec.http.HttpHeaders

package object util extends LazyLogging {

  implicit class HttpStringBuilder(val buff: jl.StringBuilder) extends AnyVal {

    def appendHttpHeaders(headers: HttpHeaders): jl.StringBuilder = {
      headers.asScala.foreach { entry =>
        buff.append(entry.getKey).append(": ").append(entry.getValue).append(Eol)
      }
      buff
    }

    def appendRequest(result: HttpResult, charset: Charset): jl.StringBuilder = {
      val request = result.request
      buff.append(request.getMethod).append(" ").append(request.getUri.toUrl).append(Eol)

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

      Option(request.getBody).foreach { requestBody =>
        buff.append("stringBody=").append(requestBody).append(Eol)
      }

      if (request.getProxyServer != null) buff.append("proxy=").append(request.getProxyServer).append(Eol)

      if (request.getRealm != null) buff.append("realm=").append(request.getRealm).append(Eol)

      buff
    }

    def appendWithEol(s: String): jl.StringBuilder =
      buff.append(s).append(Eol)

    def appendResponse(result: HttpResult): jl.StringBuilder = {

      result match {
        case response: Response =>
          buff.append("status=").append(Eol).append(response.status).append(Eol)

          if (!response.headers.isEmpty) {
            buff.append("headers= ").append(Eol)
            buff.appendHttpHeaders(response.headers).append(Eol)
          }

          if (response.body.length > 0) {
            buff.append("body=").append(Eol)
            if (isText(response.headers)) {
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
            buff.append(Eol)
          }
        case _ =>
      }

      buff
    }
  }
}
