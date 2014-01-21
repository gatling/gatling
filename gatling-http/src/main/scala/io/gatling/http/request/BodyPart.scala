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
package io.gatling.http.request

import java.io.File

import com.ning.http.multipart.{ ByteArrayPartSource, FilePart, FilePartSource, Part, StringPart }

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.{ Expression, resolveOptionalExpression, Session }
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.core.util.FileHelper.RichFile

object RawFileBodyPart {

	def apply(name: Expression[String], filePath: Expression[String]) = FileBodyPart(name, RawFileBodies.asFile(filePath))
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
	_contentType: Option[String] = None,
	_charset: String = configuration.core.encoding,
	_transferEncoding: Option[Expression[String]] = None,
	_contentId: Option[Expression[String]] = None) extends BodyPart {

	def contentType(contentType: String) = copy(_contentType = Some(contentType))
	def charset(charset: String) = copy(_charset = charset)
	def contentId(contentId: Expression[String]) = copy(_contentId = Some(contentId))
	def transferEncoding(transferEncoding: Expression[String]) = copy(_transferEncoding = Some(transferEncoding))

	def toMultiPart(session: Session): Validation[Part] =
		for {
			name <- name(session)
			string <- string(session)
			contentId <- resolveOptionalExpression(_contentId, session)
			transferEncoding <- resolveOptionalExpression(_transferEncoding, session)
		} yield {
			val part = new StringPart(name, string, _charset, contentId.getOrElse(null))
			_contentType.foreach(part.setContentType)
			transferEncoding.foreach(part.setTransferEncoding)
			part
		}
}

case class ByteArrayBodyPart(
	name: Expression[String],
	bytes: Expression[Array[Byte]],
	_contentType: Option[String] = None,
	_charset: String = configuration.core.encoding,
	_fileName: Option[Expression[String]] = None,
	_transferEncoding: Option[Expression[String]] = None,
	_contentId: Option[Expression[String]] = None) extends BodyPart {

	def contentType(contentType: String) = copy(_contentType = Some(contentType))
	def charset(charset: String) = copy(_charset = charset)
	def fileName(fileName: Expression[String]) = copy(_fileName = Some(fileName))
	def contentId(contentId: Expression[String]) = copy(_contentId = Some(contentId))
	def transferEncoding(transferEncoding: Expression[String]) = copy(_transferEncoding = Some(transferEncoding))

	def toMultiPart(session: Session): Validation[Part] =
		for {
			name <- name(session)
			bytes <- bytes(session)
			fileName <- resolveOptionalExpression(_fileName, session)
			contentId <- resolveOptionalExpression(_contentId, session)
			transferEncoding <- resolveOptionalExpression(_transferEncoding, session)
		} yield {
			val source = new ByteArrayPartSource(fileName.getOrElse(null), bytes)
			val part = new FilePart(name, source, _contentType.getOrElse(null), _charset, contentId.getOrElse(null))
			transferEncoding.foreach(part.setTransferEncoding)
			part
		}
}

case class FileBodyPart(
	name: Expression[String],
	file: Expression[File],
	_contentType: Option[String] = None,
	_charset: String = configuration.core.encoding,
	_fileName: Option[Expression[String]] = None,
	_transferEncoding: Option[Expression[String]] = None,
	_contentId: Option[Expression[String]] = None) extends BodyPart {

	def contentType(contentType: String) = copy(_contentType = Some(contentType))
	def charset(charset: String) = copy(_charset = charset)
	def fileName(fileName: Expression[String]) = copy(_fileName = Some(fileName))
	def contentId(contentId: Expression[String]) = copy(_contentId = Some(contentId))
	def transferEncoding(transferEncoding: Expression[String]) = copy(_transferEncoding = Some(transferEncoding))

	def toMultiPart(session: Session): Validation[Part] =
		for {
			file <- file(session)
			validatedFile <- file.validateExistingReadable
			name <- name(session)
			fileName <- resolveOptionalExpression(_fileName, session)
			contentId <- resolveOptionalExpression(_contentId, session)
			transferEncoding <- resolveOptionalExpression(_transferEncoding, session)
		} yield {
			val source = new FilePartSource(fileName.getOrElse(null), validatedFile)
			val part = new FilePart(name, source, _contentType.getOrElse(null), _charset, contentId.getOrElse(null))
			transferEncoding.foreach(part.setTransferEncoding)
			part
		}
}
