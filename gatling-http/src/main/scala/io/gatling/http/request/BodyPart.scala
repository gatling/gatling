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

import com.ning.http.client.multipart.{ ByteArrayPart, FilePart, Part, PartBase, StringPart }

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.{ Expression, RichExpression, Session, resolveOptionalExpression }
import io.gatling.core.util.IO._
import io.gatling.core.validation.Validation

object BodyPart {

  def rawFileBodyPart(name: Expression[String], filePath: Expression[String]) = fileBodyPart(name, RawFileBodies.asFile(filePath))
  def elFileBodyPart(name: Expression[String], filePath: Expression[String]) = stringBodyPart(name, ELFileBodies.asString(filePath))
  def stringBodyPart(name: Expression[String], string: Expression[String]) = BodyPart(name, stringBodyPartBuilder(string), BodyPartAttributes(charset = Some(configuration.core.encoding)))
  def byteArrayBodyPart(name: Expression[String], bytes: Expression[Array[Byte]]) = BodyPart(name, byteArrayBodyPartBuilder(bytes), BodyPartAttributes())
  def fileBodyPart(name: Expression[String], file: Expression[File]) = BodyPart(name, fileBodyPartBuilder(file), BodyPartAttributes())

  private def stringBodyPartBuilder(string: Expression[String])(name: String, contentType: Option[String], charset: Option[String], fileName: Option[String], contentId: Option[String]): Expression[PartBase] =
    fileName match {
      case None => string.map(resolvedString => new StringPart(name, resolvedString, configuration.core.encoding))
      case _    => byteArrayBodyPartBuilder(string.map(_.getBytes(configuration.core.charset)))(name, contentType, charset, fileName, contentId)
    }

  private def byteArrayBodyPartBuilder(bytes: Expression[Array[Byte]])(name: String, contentType: Option[String], charset: Option[String], fileName: Option[String], contentId: Option[String]): Expression[PartBase] =
    bytes.map { resolvedBytes =>
      new ByteArrayPart(name, resolvedBytes, contentType.orNull, charset.orNull, fileName.orNull, contentId.orNull)
    }

  private def fileBodyPartBuilder(file: Expression[File])(name: String, contentType: Option[String], charset: Option[String], fileName: Option[String], contentId: Option[String]): Expression[PartBase] =
    session => for {
      resolvedFile <- file(session)
      validatedFile <- resolvedFile.validateExistingReadable
    } yield new FilePart(name, validatedFile, contentType.orNull, charset.orNull, fileName.orNull, contentId.orNull)
}

case class BodyPartAttributes(
  contentType: Option[String] = None,
  charset: Option[String] = None,
  dispositionType: Option[String] = None,
  fileName: Option[Expression[String]] = None,
  contentId: Option[Expression[String]] = None)

case class BodyPart(
    name: Expression[String],
    partBuilder: (String, Option[String], Option[String], Option[String], Option[String]) => Expression[PartBase], // name, fileName
    attributes: BodyPartAttributes) {

  def contentType(contentType: String) = copy(attributes = attributes.copy(contentType = Some(contentType)))
  def charset(charset: String) = copy(attributes = attributes.copy(charset = Some(charset)))
  def dispositionType(dispositionType: String) = copy(attributes = attributes.copy(dispositionType = Some(dispositionType)))
  def fileName(fileName: Expression[String]) = copy(attributes = attributes.copy(fileName = Some(fileName)))
  def contentId(contentId: Expression[String]) = copy(attributes = attributes.copy(contentId = Some(contentId)))

  def toMultiPart(session: Session): Validation[Part] =
    for {
      name <- name(session)
      fileName <- resolveOptionalExpression(attributes.fileName, session)
      contentId <- resolveOptionalExpression(attributes.contentId, session)
      part <- partBuilder(name, attributes.contentType, attributes.charset, fileName, contentId)(session)
    } yield {
      attributes.dispositionType.foreach(part.setDispositionType)
      part
    }
}
