/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http

import java.lang.{ StringBuilder => JStringBuilder }
import java.util.{ List => JList, Map => JMap }

import scala.collection.JavaConversions.{ asScalaBuffer, asScalaSet, collectionAsScalaIterable }

import com.ning.http.client.{ Param, Request }
import com.ning.http.client.multipart._

import io.gatling.core.util.StringHelper.Eol
import io.gatling.http.response.Response

package object util {

  implicit class HttpStringBuilder(val buff: JStringBuilder) extends AnyVal {

    def appendAHCStringsMap(map: JMap[String, JList[String]]): JStringBuilder =
      map.entrySet.foldLeft(buff) { (buf, entry) =>
        buff.append(entry.getKey).append(": ").append(entry.getValue).append(Eol)
      }

    def appendParamJList(list: JList[Param]): JStringBuilder =
      list.foldLeft(buff) { (buf, param) =>
        buff.append(param.getName).append(": ").append(param.getValue).append(Eol)
      }

    def appendAHCRequest(request: Request): JStringBuilder = {

      buff.append(request.getMethod).append(" ").append(request.getURI.toUrl).append(Eol)

      if (request.getHeaders != null && !request.getHeaders.isEmpty) {
        buff.append("headers=").append(Eol)
        buff.appendAHCStringsMap(request.getHeaders)
      }

      if (!request.getCookies.isEmpty) {
        buff.append("cookies=").append(Eol)
        for (cookie <- request.getCookies) {
          buff.append(cookie).append(Eol)
        }
      }

      if (request.getFormParams != null && !request.getFormParams.isEmpty) {
        buff.append("params=").append(Eol)
        buff.appendParamJList(request.getFormParams)
      }

      if (request.getStringData != null) buff.append("stringData=").append(request.getStringData).append(Eol)

      if (request.getByteData != null) buff.append("byteData.length=").append(request.getByteData.length).append(Eol)

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
      }

      buff
    }

    def appendResponse(response: Response) = {

      response.status.foreach { status =>
        buff.append("status=").append(Eol).append(status.getStatusCode).append(" ").append(status.getStatusText).append(Eol)

        if (!response.headers.isEmpty) {
          buff.append("headers= ").append(Eol)
          buff.appendAHCStringsMap(response.headers).append(Eol)
        }

        if (response.hasResponseBody)
          buff.append("body=").append(Eol).append(response.body.string)
      }

      buff
    }
  }
}
