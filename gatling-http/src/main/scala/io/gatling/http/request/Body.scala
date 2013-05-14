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
package io.gatling.http.request

import java.io.{ File => JFile, InputStream }

import com.ning.http.client.RequestBuilder
import com.ning.http.client.generators.InputStreamBodyGenerator

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.Validation

trait Body {
	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder]
}

case class StringBody(string: Expression[String]) extends Body {
	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = string(session).map(requestBuilder.setBody)
}

case class RawFileBody(file: Expression[JFile]) extends Body {
	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = file(session).map(requestBuilder.setBody)
}

case class ByteArrayBody(byteArray: Expression[Array[Byte]]) extends Body {
	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = byteArray(session).map(requestBuilder.setBody)
}

case class InputStreamBody(is: Expression[InputStream]) extends Body {
	def setBody(requestBuilder: RequestBuilder, session: Session): Validation[RequestBuilder] = is(session).map(is => requestBuilder.setBody(new InputStreamBodyGenerator(is)))
}
