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
package io.gatling.http

import java.lang.{ StringBuilder => JStringBuilder }
import java.nio.charset.Charset
import java.util.{ List => JList }

import scala.collection.JavaConversions._
import scala.util.control.NonFatal

import io.gatling.commons.util.StringHelper.Eol
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper.isTxt

import com.typesafe.scalalogging.LazyLogging
import io.netty.buffer.ByteBufAllocator
import io.netty.handler.codec.http.HttpHeaders
import org.asynchttpclient.netty.request.NettyRequest
import org.asynchttpclient.netty.request.body.NettyMultipartBody
import org.asynchttpclient.{ Param, Request }
import org.asynchttpclient.request.body.multipart._

package object util extends LazyLogging {

  implicit class HttpStringBuilder(val buff: JStringBuilder) extends AnyVal {

    def appendHttpHeaders(headers: HttpHeaders): JStringBuilder =
      headers.foldLeft(buff) { (buf, entry) =>
        buff.append(entry.getKey).append(": ").append(entry.getValue).append(Eol)
      }

    def appendParamJList(list: JList[Param]): JStringBuilder =
      list.foldLeft(buff) { (buf, param) =>
        buff.append(param.getName).append(": ").append(param.getValue).append(Eol)
      }

    def appendRequest(request: Request, nettyRequest: Option[NettyRequest], charset: Charset): JStringBuilder = {

      buff.append(request.getMethod).append(" ").append(request.getUrl).append(Eol)

      nettyRequest match {
        case Some(nr) =>

          val headers = nr.getHttpRequest.headers
          if (!headers.isEmpty) {
            buff.append("headers=").append(Eol)
            for (header <- headers) {
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
            for (cookie <- request.getCookies) {
              buff.append(cookie).append(Eol)
            }
          }
      }

      if (!request.getFormParams.isEmpty) {
        buff.append("params=").append(Eol)
        buff.appendParamJList(request.getFormParams)
      }

      if (request.getStringData != null) buff.append("stringData=").append(request.getStringData).append(Eol)

      if (request.getByteData != null) buff.append("byteData=").append(new String(request.getByteData, charset)).append(Eol)

      if (request.getCompositeByteData != null) {
        buff.append("compositeByteData=")
        request.getCompositeByteData.foreach(b => buff.append(new String(b, charset)))
        buff.append(Eol)
      }

      if (request.getFile != null) buff.append("file=").append(request.getFile.getCanonicalPath).append(Eol)

      if (!request.getBodyParts.isEmpty) {
        buff.append("parts=").append(Eol)
        request.getBodyParts.foreach {
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
              .append(" file=").append(part.getFile.getCanonicalPath)
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

        buff.append("multipart=").append(Eol)

        val multipartBody = nettyRequest match {
          case Some(req) =>
            val originalMultipartBody = req.getBody.asInstanceOf[NettyMultipartBody].getBody.asInstanceOf[MultipartBody]
            val multipartParts = MultipartUtils.generateMultipartParts(request.getBodyParts, originalMultipartBody.getBoundary)
            new MultipartBody(multipartParts, originalMultipartBody.getContentType, originalMultipartBody.getBoundary)

          case None => MultipartUtils.newMultipartBody(request.getBodyParts, request.getHeaders)
        }

        val byteBuf = ByteBufAllocator.DEFAULT.buffer(8 * 1024)
        multipartBody.transferTo(byteBuf)
        buff.append(byteBuf.toString(charset))
        multipartBody.close()
        byteBuf.release()
      }

      if (request.getProxyServer != null) buff.append("proxy=").append(request.getProxyServer).append(Eol)

      if (request.getRealm != null) buff.append("realm=").append(request.getRealm).append(Eol)

      buff
    }

    def appendResponse(response: Response) = {

      response.status.foreach { status =>
        buff.append("status=").append(Eol).append(status.getStatusCode).append(" ").append(status.getStatusText).append(Eol)

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
                buff.append(s"$message: ${t.getMessage}")
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
