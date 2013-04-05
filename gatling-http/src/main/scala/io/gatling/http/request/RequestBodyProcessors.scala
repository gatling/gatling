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
package io.gatling.http.request

import java.io.FileInputStream

import io.gatling.core.session.Session
import io.gatling.core.util.IOHelper.withCloseable
import io.gatling.http.util.GZIPHelper

object RequestBodyProcessors {

	val gzip = (body: HttpRequestBody) => body match {

		case ByteArrayBody(byteArray) =>
			val gzippedBytes = (session: Session) => byteArray(session).map(GZIPHelper.inflate)
			ByteArrayBody(gzippedBytes)

		case RawFileBody(file) =>
			val gzippedBytes = (session: Session) => file(session).map { file =>
				withCloseable(new FileInputStream(file))(GZIPHelper.inflate)
			}
			ByteArrayBody(gzippedBytes)
		case InputStreamBody(inputStream) =>
			val gzippedBytes = (session: Session) => inputStream(session).map { is =>
				withCloseable(is)(GZIPHelper.inflate)
			}
			ByteArrayBody(gzippedBytes)

		case body => throw new IllegalArgumentException(s"Body $body is not supported by requestCompressor")
	}
}