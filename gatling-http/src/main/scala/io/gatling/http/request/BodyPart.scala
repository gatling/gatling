/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request

import java.nio.charset.Charset

import io.gatling.commons.util.Io._
import io.gatling.commons.validation.Validation
import io.gatling.core.body.{ FileWithCachedBytes, ElFileBodies, RawFileBodies }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._

import com.softwaremill.quicklens._
import org.asynchttpclient.request.body.multipart.{ ByteArrayPart, FilePart, Part, PartBase, StringPart }

object BodyPart {

  def rawFileBodyPart(name: Option[Expression[String]], filePath: Expression[String])(implicit rawFileBodies: RawFileBodies): BodyPart =
    BodyPart(name, fileBodyPartBuilder(rawFileBodies.asFileWithCachedBytes(filePath)), BodyPartAttributes())

  def elFileBodyPart(name: Option[Expression[String]], filePath: Expression[String])(implicit configuration: GatlingConfiguration, elFileBodies: ElFileBodies): BodyPart =
    stringBodyPart(name, elFileBodies.asString(filePath))

  def stringBodyPart(name: Option[Expression[String]], string: Expression[String])(implicit configuration: GatlingConfiguration): BodyPart =
    BodyPart(name, stringBodyPartBuilder(string), BodyPartAttributes(charset = Some(configuration.core.charset)))

  def byteArrayBodyPart(name: Option[Expression[String]], bytes: Expression[Array[Byte]]): BodyPart = BodyPart(name, byteArrayBodyPartBuilder(bytes), BodyPartAttributes())

  private def stringBodyPartBuilder(string: Expression[String])(name: String, contentType: Option[String], charset: Option[Charset], fileName: Option[String], contentId: Option[String], transferEncoding: Option[String]): Expression[PartBase] =
    fileName match {
      case None => string.map { resolvedString =>
        new StringPart(name, resolvedString, contentType.orNull, charset.orNull, contentId.orNull, transferEncoding.orNull)
      }
      case _ => byteArrayBodyPartBuilder(string.map(_.getBytes(charset.orNull)))(name, contentType, charset, fileName, contentId, transferEncoding)
    }

  private def byteArrayBodyPartBuilder(bytes: Expression[Array[Byte]])(name: String, contentType: Option[String], charset: Option[Charset], fileName: Option[String], contentId: Option[String], transferEncoding: Option[String]): Expression[PartBase] =
    bytes.map { resolvedBytes =>
      new ByteArrayPart(name, resolvedBytes, contentType.orNull, charset.orNull, fileName.orNull, contentId.orNull, transferEncoding.orNull)
    }

  private def fileBodyPartBuilder(file: Expression[FileWithCachedBytes])(name: String, contentType: Option[String], charset: Option[Charset], fileName: Option[String], contentId: Option[String], transferEncoding: Option[String]): Expression[PartBase] =
    session => for {
      resolvedFile <- file(session)
      validatedFile <- resolvedFile.file.validateExistingReadable
    } yield resolvedFile.cachedBytes match {
      case Some(bytes) => new ByteArrayPart(name, bytes, contentType.orNull, charset.orNull, fileName.getOrElse(validatedFile.getName), contentId.orNull, transferEncoding.orNull)
      case None        => new FilePart(name, validatedFile, contentType.orNull, charset.orNull, fileName.getOrElse(validatedFile.getName), contentId.orNull, transferEncoding.orNull)
    }
}

case class BodyPartAttributes(
    contentType:      Option[Expression[String]]         = None,
    charset:          Option[Charset]                    = None,
    dispositionType:  Option[Expression[String]]         = None,
    fileName:         Option[Expression[String]]         = None,
    contentId:        Option[Expression[String]]         = None,
    transferEncoding: Option[String]                     = None,
    customHeaders:    List[(String, Expression[String])] = Nil
) {

  lazy val customHeadersExpression: Expression[Seq[(String, String)]] = resolveIterable(customHeaders)
}

case class BodyPart(
    name:        Option[Expression[String]],
    partBuilder: (String, Option[String], Option[Charset], Option[String], Option[String], Option[String]) => Expression[PartBase], // name, fileName
    attributes:  BodyPartAttributes
) {

  def contentType(contentType: Expression[String]) = this.modify(_.attributes.contentType).setTo(Some(contentType))
  def charset(charset: String) = this.modify(_.attributes.charset).setTo(Some(Charset.forName(charset)))
  def dispositionType(dispositionType: Expression[String]) = this.modify(_.attributes.dispositionType).setTo(Some(dispositionType))
  def fileName(fileName: Expression[String]) = this.modify(_.attributes.fileName).setTo(Some(fileName))
  def contentId(contentId: Expression[String]) = this.modify(_.attributes.contentId).setTo(Some(contentId))
  def transferEncoding(transferEncoding: String) = this.modify(_.attributes.transferEncoding).setTo(Some(transferEncoding))
  def header(name: String, value: Expression[String]) = this.modify(_.attributes.customHeaders).using(_ ::: List(name -> value))

  def toMultiPart(session: Session): Validation[Part] =
    for {
      name <- resolveOptionalExpression(name, session)
      contentType <- resolveOptionalExpression(attributes.contentType, session)
      dispositionType <- resolveOptionalExpression(attributes.dispositionType, session)
      fileName <- resolveOptionalExpression(attributes.fileName, session)
      contentId <- resolveOptionalExpression(attributes.contentId, session)
      part <- partBuilder(name.orNull, contentType, attributes.charset, fileName, contentId, attributes.transferEncoding)(session)
      customHeaders <- attributes.customHeadersExpression(session)

    } yield {
      dispositionType.foreach(part.setDispositionType)
      customHeaders.foreach { case (headerName, headerValue) => part.addCustomHeader(headerName, headerValue) }
      part
    }
}
