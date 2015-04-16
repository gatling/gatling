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
package io.gatling.http

import java.lang.{ StringBuilder => JStringBuilder }
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.{ List => JList, Map => JMap }

import scala.collection.JavaConversions._

import com.ning.http.client.{ Param, Request }
import com.ning.http.client.multipart._
import com.ning.http.client.providers.netty.request.NettyRequest
import com.ning.http.client.providers.netty.request.body.NettyMultipartBody

import io.gatling.core.util.StringHelper.Eol
import io.gatling.http.response.Response

package object util {

  implicit class HttpStringBuilder(val buff: JStringBuilder) extends AnyVal {

    def appendAhcStringsMap(map: JMap[String, JList[String]]): JStringBuilder =
      map.entrySet.foldLeft(buff) { (buf, entry) =>
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
          if (request.getHeaders != null && !request.getHeaders.isEmpty) {
            buff.append("headers=").append(Eol)
            buff.appendAhcStringsMap(request.getHeaders)
          }

          if (!request.getCookies.isEmpty) {
            buff.append("cookies=").append(Eol)
            for (cookie <- request.getCookies) {
              buff.append(cookie).append(Eol)
            }
          }
      }

      if (request.getFormParams != null && !request.getFormParams.isEmpty) {
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

      if (request.getFile != null) buff.append("file=").append(request.getFile.getAbsolutePath).append(Eol)

      if (request.getParts != null && !request.getParts.isEmpty) {
        buff.append("parts=").append(Eol)
        request.getParts.foreach {
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
              .append(" file=").append(part.getFile.getAbsolutePath)
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
            new MultipartBody(request.getParts, originalMultipartBody.getContentType, originalMultipartBody.getContentLength, originalMultipartBody.getBoundary)

          case None => MultipartUtils.newMultipartBody(request.getParts, request.getHeaders)
        }

        val byteBuffer = ByteBuffer.allocate(8 * 1024)
        multipartBody.read(byteBuffer)
        byteBuffer.flip()
        buff.append(charset.decode(byteBuffer).toString)
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
          buff.appendAhcStringsMap(response.headers).append(Eol)
        }

        if (response.hasResponseBody)
          buff.append("body=").append(Eol).append(response.body.string)
      }

      buff
    }
  }
}
