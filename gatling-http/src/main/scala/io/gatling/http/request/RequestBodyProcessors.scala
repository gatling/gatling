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

import java.io.{ BufferedInputStream, ByteArrayInputStream, FileInputStream }

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.Session
import io.gatling.core.util.IOHelper.withCloseable
import io.gatling.http.util.GZIPHelper

object RequestBodyProcessors {

	val gzipRequestBody = (body: HttpRequestBody) => {
		val gzippedBytes = body match {

			case StringBody(string) => (session: Session) => string(session).map(GZIPHelper.gzip)

			case ByteArrayBody(byteArray) => (session: Session) => byteArray(session).map(GZIPHelper.gzip)

			case RawFileBody(file) => (session: Session) => file(session).map { file =>
				withCloseable(new FileInputStream(file))(GZIPHelper.gzip)
			}
			case InputStreamBody(inputStream) => (session: Session) => inputStream(session).map { is =>
				withCloseable(is)(GZIPHelper.gzip)
			}
			case body => throw new IllegalArgumentException(s"Body $body is not supported by requestCompressor")
		}

		ByteArrayBody(gzippedBytes)
	}

	val streamRequestBody = (body: HttpRequestBody) => {
		val stream = body match {

			case StringBody(string) => (session: Session) => string(session).map(s => new BufferedInputStream(new ByteArrayInputStream(s.getBytes(configuration.simulation.encoding))))

			case ByteArrayBody(byteArray) => (session: Session) => byteArray(session).map(b => new BufferedInputStream(new ByteArrayInputStream(b)))

			case RawFileBody(file) => (session: Session) => file(session).map(f => new BufferedInputStream(new FileInputStream(f)))

			case InputStreamBody(inputStream) => inputStream

			case body => throw new IllegalArgumentException(s"Body $body is not supported by streamRequestBody")
		}
		InputStreamBody(stream)
	}
}