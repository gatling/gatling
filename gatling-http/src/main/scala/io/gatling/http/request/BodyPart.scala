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

import java.io.File

import com.ning.http.multipart.{ ByteArrayPartSource, FilePart, FilePartSource, Part, StringPart }

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.{ Expression, resolveOptionalExpression, Session }
import io.gatling.core.validation.{ SuccessWrapper, Validation }

object RawFileBodyPart {

	def apply(name: Expression[String], filePath: Expression[String], contentType: String) = FileBodyPart(name, RawFileBodies.asFile(filePath), contentType)
}

object ELFileBodyPart {

	def apply(name: Expression[String], filePath: Expression[String]) = StringBodyPart(name, ELFileBodies.asString(filePath))
}

sealed trait BodyPart {

	def toMultiPart(session: Session): Validation[Part]
}

case class StringBodyPart(
	name: Expression[String],
	string: Expression[String],
	charset: String = configuration.core.encoding,
	contentType: Option[String] = None,
	transferEncoding: Option[String] = None,
	contentId: Option[String] = None) extends BodyPart {

	def withCharset(charset: String) = copy(charset = charset)
	def withContentId(contentId: String) = copy(contentId = Some(contentId))
	def withContentType(contentType: String) = copy(contentType = Some(contentType))
	def withTransferEncoding(transferEncoding: String) = copy(transferEncoding = Some(transferEncoding))

	def toMultiPart(session: Session): Validation[Part] =
		for {
			name <- name(session)
			string <- string(session)
		} yield {
			val part = new StringPart(name, string, charset, contentId.getOrElse(null))
			contentType.map(part.setContentType)
			transferEncoding.map(part.setTransferEncoding)
			part
		}
}

case class ByteArrayBodyPart(
	name: Expression[String],
	bytes: Expression[Array[Byte]],
	contentType: String,
	charset: String = configuration.core.encoding,
	fileName: Option[String] = None,
	transferEncoding: Option[String] = None,
	contentId: Option[String] = None) extends BodyPart {

	def withCharset(charset: String) = copy(charset = charset)
	def withFileName(fileName: String) = copy(fileName = Some(fileName))
	def withContentId(contentId: String) = copy(contentId = Some(contentId))
	def withTransferEncoding(transferEncoding: String) = copy(transferEncoding = Some(transferEncoding))

	def toMultiPart(session: Session): Validation[Part] =
		for {
			name <- name(session)
			bytes <- bytes(session)
		} yield {
			val source = new ByteArrayPartSource(fileName.getOrElse(null), bytes)
			val part = new FilePart(name, source, contentType, charset, contentId.getOrElse(null))
			transferEncoding.map(part.setTransferEncoding)
			part
		}
}

case class FileBodyPart(
	name: Expression[String],
	file: Expression[File],
	contentType: String,
	charset: String = configuration.core.encoding,
	fileName: Option[Expression[String]] = None,
	transferEncoding: Option[Expression[String]] = None,
	contentId: Option[Expression[String]] = None) extends BodyPart {

	def withCharset(charset: String) = copy(charset = charset)
	def withFileName(fileName: Expression[String]) = copy(fileName = Some(fileName))
	def withContentId(contentId: Expression[String]) = copy(contentId = Some(contentId))
	def withTransferEncoding(transferEncoding: Expression[String]) = copy(transferEncoding = Some(transferEncoding))

	def toMultiPart(session: Session): Validation[Part] = {
		
		for {
			name <- name(session)
			file <- file(session)
			fileName <- resolveOptionalExpression(fileName, session)
			contentId <- resolveOptionalExpression(contentId, session)
			transferEncoding <- resolveOptionalExpression(transferEncoding, session)
		} yield {
			val source = new FilePartSource(fileName.getOrElse(null), file)
			val part = new FilePart(name, source, contentType, charset, contentId.getOrElse(null))
			transferEncoding.map(part.setTransferEncoding)
			part
		}
	}
}