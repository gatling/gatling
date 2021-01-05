/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import java.{ util => ju }
import java.nio.charset.Charset

import io.gatling.commons.validation.Validation
import io.gatling.core.body._
import io.gatling.core.session._
import io.gatling.http.client.Param
import io.gatling.http.client.body.multipart._

import com.softwaremill.quicklens._

object BodyPart {

  def rawFileBodyPart(
      name: Option[Expression[String]],
      filePath: Expression[String],
      rawFileBodies: RawFileBodies
  ): BodyPart =
    BodyPart(name, fileBodyPartBuilder(rawFileBodies.asResourceAndCachedBytes(filePath)), BodyPartAttributes.Empty)

  def elFileBodyPart(
      name: Option[Expression[String]],
      filePath: Expression[String],
      defaultCharset: Charset,
      elFileBodies: ElFileBodies
  ): BodyPart =
    stringBodyPart(name, new ElBody(elFileBodies.parse(filePath)), defaultCharset)

  def pebbleStringBodyPart(name: Option[Expression[String]], string: String, defaultCharset: Charset): BodyPart =
    stringBodyPart(name, PebbleStringBody(string, defaultCharset), defaultCharset)

  def pebbleFileBodyPart(
      name: Option[Expression[String]],
      filePath: Expression[String],
      defaultCharset: Charset,
      pebbleFileBodies: PebbleFileBodies
  ): BodyPart =
    stringBodyPart(name, PebbleFileBody(filePath, pebbleFileBodies, defaultCharset), defaultCharset)

  def stringBodyPart(name: Option[Expression[String]], string: Expression[String], defaultCharset: Charset): BodyPart =
    BodyPart(name, stringBodyPartBuilder(string, defaultCharset), BodyPartAttributes.Empty)

  def byteArrayBodyPart(name: Option[Expression[String]], bytes: Expression[Array[Byte]]): BodyPart =
    BodyPart(name, byteArrayBodyPartBuilder(bytes), BodyPartAttributes.Empty)

  private def stringBodyPartBuilder(string: Expression[String], defaultCharset: Charset)(
      name: String,
      charset: Option[Charset],
      transferEncoding: Option[String],
      contentId: Option[String],
      dispositionType: Option[String],
      contentType: Option[String],
      customHeaders: ju.List[Param],
      fileName: Option[String]
  ): Expression[Part[_]] =
    fileName match {
      case None =>
        string.map { resolvedString =>
          new StringPart(
            name,
            resolvedString,
            charset.getOrElse(defaultCharset),
            transferEncoding.orNull,
            contentId.orNull,
            dispositionType.orNull,
            contentType.orNull,
            customHeaders
          )
        }
      case _ =>
        byteArrayBodyPartBuilder(string.map(_.getBytes(charset.getOrElse(defaultCharset))))(
          name,
          charset,
          transferEncoding,
          contentId,
          dispositionType,
          contentType,
          customHeaders,
          fileName
        )
    }

  private def byteArrayBodyPartBuilder(bytes: Expression[Array[Byte]])(
      name: String,
      charset: Option[Charset],
      transferEncoding: Option[String],
      contentId: Option[String],
      dispositionType: Option[String],
      contentType: Option[String],
      customHeaders: ju.List[Param],
      fileName: Option[String]
  ): Expression[Part[_]] =
    bytes.map { resolvedBytes =>
      new ByteArrayPart(
        name,
        resolvedBytes,
        charset.orNull,
        transferEncoding.orNull,
        contentId.orNull,
        dispositionType.orNull,
        contentType.orNull,
        customHeaders,
        fileName.orNull
      )
    }

  private def fileBodyPartBuilder(resource: Expression[ResourceAndCachedBytes])(
      name: String,
      charset: Option[Charset],
      transferEncoding: Option[String],
      contentId: Option[String],
      dispositionType: Option[String],
      contentType: Option[String],
      customHeaders: ju.List[Param],
      fileName: Option[String]
  ): Expression[Part[_]] =
    session =>
      for {
        ResourceAndCachedBytes(resource, cachedBytes) <- resource(session)
      } yield cachedBytes match {
        case Some(bytes) =>
          new ByteArrayPart(
            name,
            bytes,
            charset.orNull,
            transferEncoding.orNull,
            contentId.orNull,
            dispositionType.orNull,
            contentType.orNull,
            customHeaders,
            fileName.getOrElse(resource.name)
          )
        case _ =>
          new FilePart(
            name,
            resource.file,
            charset.orNull,
            transferEncoding.orNull,
            contentType.orNull,
            dispositionType.orNull,
            contentId.orNull,
            customHeaders,
            fileName.getOrElse(resource.name)
          )
      }
}

object BodyPartAttributes {
  val Empty: BodyPartAttributes = BodyPartAttributes(None, None, None, None, None, None, Nil)
}

final case class BodyPartAttributes(
    contentType: Option[Expression[String]],
    charset: Option[Charset],
    dispositionType: Option[Expression[String]],
    fileName: Option[Expression[String]],
    contentId: Option[Expression[String]],
    transferEncoding: Option[String],
    customHeaders: List[(String, Expression[String])]
) {

  lazy val customHeadersExpression: Expression[Seq[(String, String)]] = resolveIterable(customHeaders)
}

final case class BodyPart(
    name: Option[Expression[String]],
    partBuilder: (
        String, // name
        Option[Charset], // charset
        Option[String], // transferEncoding
        Option[String], // contentId
        Option[String], // dispositionType
        Option[String], // contentType
        ju.List[Param], // customHeaders
        Option[String] // fileName
    ) => Expression[Part[_]],
    attributes: BodyPartAttributes
) {

  def contentType(contentType: Expression[String]): BodyPart = this.modify(_.attributes.contentType).setTo(Some(contentType))

  def charset(charset: String): BodyPart = this.modify(_.attributes.charset).setTo(Some(Charset.forName(charset)))

  def dispositionType(dispositionType: Expression[String]): BodyPart = this.modify(_.attributes.dispositionType).setTo(Some(dispositionType))

  def fileName(fileName: Expression[String]): BodyPart = this.modify(_.attributes.fileName).setTo(Some(fileName))

  def contentId(contentId: Expression[String]): BodyPart = this.modify(_.attributes.contentId).setTo(Some(contentId))

  def transferEncoding(transferEncoding: String): BodyPart = this.modify(_.attributes.transferEncoding).setTo(Some(transferEncoding))

  def header(name: String, value: Expression[String]): BodyPart = this.modify(_.attributes.customHeaders).using(_ ::: List(name -> value))

  def toMultiPart(session: Session): Validation[Part[_]] =
    for {
      name <- resolveOptionalExpression(name, session)
      contentType <- resolveOptionalExpression(attributes.contentType, session)
      dispositionType <- resolveOptionalExpression(attributes.dispositionType, session)
      fileName <- resolveOptionalExpression(attributes.fileName, session)
      contentId <- resolveOptionalExpression(attributes.contentId, session)
      customHeaders <- attributes.customHeadersExpression(session)
      customHeadersAsParams =
        if (customHeaders.nonEmpty) {
          val params = new ju.ArrayList[Param](customHeaders.size)
          customHeaders.foreach { case (headerName, value) => params.add(new Param(headerName, value)) }
          params
        } else {
          ju.Collections.emptyList[Param]
        }
      part <- partBuilder(
        name.orNull,
        attributes.charset,
        attributes.transferEncoding,
        contentId,
        dispositionType,
        contentType,
        customHeadersAsParams,
        fileName
      )(session)

    } yield part
}
