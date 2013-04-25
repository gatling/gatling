/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package com.excilys.ebi.gatling.http.request

import scala.collection.JavaConversions.{ asScalaBuffer, collectionAsScalaIterable }

import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE
import com.excilys.ebi.gatling.http.util.HttpHelper.dumpFluentCaseInsensitiveStringsMap
import com.ning.http.client.{ ByteArrayPart, FilePart, Request, StringPart }
import com.ning.http.multipart.{ FilePart => MultipartFilePart, StringPart => MultipartStringPart }

object ExtendedRequest {
	implicit def extendRequest(request: Request): ExtendedRequest = new ExtendedRequest(request)
}

class ExtendedRequest(request: Request) {

	def dumpTo(buff: StringBuilder) {

		buff.append(request.getMethod).append(" ").append(if (request.isUseRawUrl) request.getRawUrl else request.getUrl).append(END_OF_LINE)

		if (request.getHeaders != null && !request.getHeaders.isEmpty) {
			buff.append("headers=").append(END_OF_LINE)
			dumpFluentCaseInsensitiveStringsMap(request.getHeaders, buff)
		}

		if (request.getCookies != null && !request.getCookies.isEmpty) {
			buff.append("cookies=").append(END_OF_LINE)
			for (cookie <- request.getCookies) {
				buff.append(cookie).append(END_OF_LINE)
			}
		}

		if (request.getParams != null && !request.getParams.isEmpty) {
			buff.append("params=").append(END_OF_LINE)
			dumpFluentCaseInsensitiveStringsMap(request.getParams, buff)
		}

		if (request.getStringData != null) buff.append("stringData=").append(request.getStringData).append(END_OF_LINE)

		if (request.getByteData != null) buff.append("byteData.length=").append(request.getByteData.length).append(END_OF_LINE)

		if (request.getFile != null) buff.append("file=").append(request.getFile.getAbsolutePath).append(END_OF_LINE)

		if (request.getParts != null && !request.getParts.isEmpty) {
			buff.append("parts=").append(END_OF_LINE)
			request.getParts.foreach {
				_ match {
					case byteArrayPart: ByteArrayPart => buff.append("byteArrayPart: name=").append(byteArrayPart.getName).append(END_OF_LINE)
					case filePart: FilePart => buff.append("filePart: name=").append(filePart.getName).append(" file=").append(filePart.getFile.getAbsolutePath).append(END_OF_LINE)
					case stringPart: StringPart => buff.append("stringPart: name=").append(stringPart.getName).append(" string=").append(stringPart.getValue).append(END_OF_LINE)
					case multipartFilePart: MultipartFilePart => buff.append("multipartFilePart: name=").append(multipartFilePart.getName).append(END_OF_LINE)
					case multipartStringPart: MultipartStringPart => buff.append("multipartStringPart: name=").append(multipartStringPart.getName).append(END_OF_LINE)
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