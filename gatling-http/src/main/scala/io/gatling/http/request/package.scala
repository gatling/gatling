/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import scala.collection.JavaConversions.{ asScalaBuffer, collectionAsScalaIterable }

import io.gatling.core.session.Session
import io.gatling.core.util.StringHelper.eol
import io.gatling.core.validation.Validation
import io.gatling.http.request.{ ByteArrayBody, HttpRequestBody, RawFileBody }
import io.gatling.http.util.HttpHelper.dumpFluentCaseInsensitiveStringsMap
import com.ning.http.client.{ ByteArrayPart, FilePart, Request, RequestBuilder, StringPart }
import com.ning.http.client.generators.InputStreamBodyGenerator
import com.ning.http.multipart.{ FilePart => MultipartFilePart, StringPart => MultipartStringPart }

package object request {

	implicit class ExtendedRequest(val request: Request) extends AnyVal {

		def dumpTo(buff: StringBuilder) {

			buff.append(request.getMethod).append(" ").append(if (request.isUseRawUrl) request.getRawUrl else request.getUrl).append(eol)

			if (request.getHeaders != null && !request.getHeaders.isEmpty) {
				buff.append("headers=").append(eol)
				dumpFluentCaseInsensitiveStringsMap(request.getHeaders, buff)
			}

			if (request.getCookies != null && !request.getCookies.isEmpty) {
				buff.append("cookies=").append(eol)
				for (cookie <- request.getCookies) {
					buff.append(cookie).append(eol)
				}
			}

			if (request.getParams != null && !request.getParams.isEmpty) {
				buff.append("params=").append(eol)
				dumpFluentCaseInsensitiveStringsMap(request.getParams, buff)
			}

			if (request.getStringData != null) buff.append("stringData=").append(request.getStringData).append(eol)

			if (request.getByteData != null) buff.append("byteData.length=").append(request.getByteData.length).append(eol)

			if (request.getFile != null) buff.append("file=").append(request.getFile.getAbsolutePath).append(eol)

			if (request.getParts != null && !request.getParts.isEmpty) {
				buff.append("parts=").append(eol)
				request.getParts.foreach {
					_ match {
						case byteArrayPart: ByteArrayPart => buff.append("byteArrayPart: name=").append(byteArrayPart.getName).append(eol)
						case filePart: FilePart => buff.append("filePart: name=").append(filePart.getName).append(" file=").append(filePart.getFile.getAbsolutePath).append(eol)
						case stringPart: StringPart => buff.append("stringPart: name=").append(stringPart.getName).append(" string=").append(stringPart.getValue).append(eol)
						case multipartFilePart: MultipartFilePart => buff.append("multipartFilePart: name=").append(multipartFilePart.getName).append(eol)
						case multipartStringPart: MultipartStringPart => buff.append("multipartStringPart: name=").append(multipartStringPart.getName).append(eol)
					}
				}
			}
		}

		def dump: StringBuilder = {
			val buff = new StringBuilder
			dumpTo(buff)
			buff
		}
	}

	implicit class HttpRequestBodySetter(val requestBuilder: RequestBuilder) extends AnyVal {

		def setBody(body: HttpRequestBody, session: Session): Validation[RequestBuilder] = body match {
			case RawFileBody(file) => file(session).map(requestBuilder.setBody)
			case ByteArrayBody(byteArray) => byteArray(session).map(requestBuilder.setBody)
			case InputStreamBody(is) => is(session).map(is => requestBuilder.setBody(new InputStreamBodyGenerator(is)))
		}
	}
}