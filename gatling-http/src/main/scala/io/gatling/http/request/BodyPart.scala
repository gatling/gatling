/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request

import java.io.File

import com.ning.http.client.{ ByteArrayPart, FilePart, Part }
import com.ning.http.multipart.StringPart

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.validation.Validation

sealed trait BodyPart {

	def toPart(session: Session): Validation[Part]
}

case class StringBodyPart(name: Expression[String], value: Expression[String]) extends BodyPart {

	def toPart(session: Session): Validation[Part] =
		for {
			name <- name(session)
			value <- value(session)
		} yield new StringPart(name, value, configuration.core.encoding)
}

case class ByteArrayBodyPart(name: Expression[String], data: Expression[Array[Byte]], mimeType: String) extends BodyPart {

	def toPart(session: Session): Validation[Part] =
		for {
			name <- name(session)
			data <- data(session)
		} yield new ByteArrayPart(name, null, data, mimeType, configuration.core.encoding)
}

case class FileBodyPart(name: Expression[String], file: Expression[File], mimeType: String) extends BodyPart {

	def toPart(session: Session): Validation[Part] =
		for {
			name <- name(session)
			file <- file(session)
		} yield new FilePart(name, file, mimeType, configuration.core.encoding)
}