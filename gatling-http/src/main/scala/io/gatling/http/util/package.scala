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

import java.util.{ List => JList, Map => JMap }

import scala.collection.JavaConversions.{ asScalaBuffer, asScalaSet, collectionAsScalaIterable }

import com.ning.http.client.Request
import com.ning.http.multipart._

import io.gatling.core.util.StringHelper.eol
import io.gatling.http.response.Response

package object util {

  implicit class HttpStringBuilder(val buff: StringBuilder) extends AnyVal {

    def appendAHCStringsMap(map: JMap[String, JList[String]]): StringBuilder = {

      for {
        entry <- map.entrySet
      } buff.append(entry.getKey).append(": ").append(entry.getValue).append(eol)

      buff
    }

    def appendAHCRequest(request: Request): StringBuilder = {

      buff.append(request.getMethod).append(" ").append(if (request.isUseRawUrl) request.getRawUrl else request.getUrl).append(eol)

      if (request.getHeaders != null && !request.getHeaders.isEmpty) {
        buff.append("headers=").append(eol)
        buff.appendAHCStringsMap(request.getHeaders)
      }

      if (request.getCookies != null && !request.getCookies.isEmpty) {
        buff.append("cookies=").append(eol)
        for (cookie <- request.getCookies) {
          buff.append(cookie).append(eol)
        }
      }

      if (request.getParams != null && !request.getParams.isEmpty) {
        buff.append("params=").append(eol)
        buff.appendAHCStringsMap(request.getParams)
      }

      if (request.getStringData != null) buff.append("stringData=").append(request.getStringData).append(eol)

      if (request.getByteData != null) buff.append("byteData.length=").append(request.getByteData.length).append(eol)

      if (request.getFile != null) buff.append("file=").append(request.getFile.getAbsolutePath).append(eol)

      if (request.getParts != null && !request.getParts.isEmpty) {
        buff.append("parts=").append(eol)
        request.getParts.foreach {
          case part: StringPart =>
            buff
              .append(s"StringPart:")
              .append(s" name=${part.getName}")
              .append(s" contentType=${part.getContentType}")
              .append(s" dispositionType=${part.getDispositionType}")
              .append(s" charset=${part.getCharSet}")
              .append(s" transferEncoding=${part.getTransferEncoding}")
              .append(s" contentId=${part.getContentId}$eol")

          case part: FilePart =>
            buff.append(s"FilePart:")
              .append(s" name=${part.getName}")
              .append(s" contentType=${part.getContentType}")
              .append(s" dispositionType=${part.getDispositionType}")
              .append(s" charset=${part.getCharSet}")
              .append(s" transferEncoding=${part.getTransferEncoding}")
              .append(s" contentId=${part.getContentId}")
              .append(s" filename=${part.getSource.getFileName}")

            part.getSource match {
              case source: FilePartSource => buff.append(s" source=File(${source.getFile.getAbsolutePath})")
              case _                      => buff.append(s" source=byte[]")
            }

            buff.append(s"$eol")
        }
      }

      buff
    }

    def appendResponse(response: Response) = {

      response.status.foreach { status =>
        buff.append("status=").append(eol).append(status.getStatusCode).append(" ").append(status.getStatusText).append(eol)

        if (!response.headers.isEmpty) {
          buff.append("headers= ").append(eol)
          buff.appendAHCStringsMap(response.headers).append(eol)
        }

        if (response.hasResponseBody)
          buff.append("body=").append(eol).append(response.body.string)
      }

      buff
    }
  }
}