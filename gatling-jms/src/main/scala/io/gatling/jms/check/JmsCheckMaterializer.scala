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

package io.gatling.jms.check

import java.nio.charset.{ Charset, StandardCharsets }
import javax.jms.{ BytesMessage, Message, TextMessage }

import io.gatling.commons.validation._
import io.gatling.core.check.{ CheckMaterializer, Preparer }
import io.gatling.core.check.bytes.BodyBytesCheckType
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.check.xpath.{ XPathCheckType, XmlParsers }
import io.gatling.core.json.JsonParsers
import io.gatling.jms.JmsCheck

import com.fasterxml.jackson.databind.JsonNode
import net.sf.saxon.s9api.XdmNode

class JmsCheckMaterializer[T, P](override val preparer: Preparer[Message, P]) extends CheckMaterializer[T, JmsCheck, Message, P](identity)

object JmsCheckMaterializer {

  private def toBytes(bytesMessage: BytesMessage): Array[Byte] = {
    val buffer = Array.ofDim[Byte](bytesMessage.getBodyLength.toInt)
    bytesMessage.readBytes(buffer)
    buffer
  }

  private def getBodyAsString(bytesMessage: BytesMessage, charset: Charset): String =
    if (charset == StandardCharsets.UTF_8)
      bytesMessage.readUTF()
    else
      new String(toBytes(bytesMessage), charset)

  private def bodyBytesPreparer(charset: Charset): Preparer[Message, Array[Byte]] = {
    case tm: TextMessage  => tm.getText.getBytes(charset).success
    case bm: BytesMessage => toBytes(bm).success
    case _                => "Unsupported message type".failure
  }

  private def bodyLengthPreparer(charset: Charset): Preparer[Message, Int] = {
    case tm: TextMessage  => tm.getText.getBytes(charset).length.success
    case bm: BytesMessage => bm.getBodyLength.toInt.success
    case _                => "Unsupported message type".failure
  }

  private val JsonPreparerErrorMapper: String => String = "Could not parse response into a JSON: " + _

  private def jsonPreparer(jsonParsers: JsonParsers, charset: Charset): Preparer[Message, JsonNode] =
    replyMessage =>
      safely(JsonPreparerErrorMapper) {
        replyMessage match {
          case tm: TextMessage  => jsonParsers.safeParse(tm.getText)
          case bm: BytesMessage => jsonParsers.safeParse(getBodyAsString(bm, charset))
          case _                => "Unsupported message type".failure
        }
      }

  private def stringBodyPreparer(charset: Charset): Preparer[Message, String] = {
    case tm: TextMessage  => tm.getText.success
    case bm: BytesMessage => getBodyAsString(bm, charset).success
    case _                => "Unsupported message type".failure
  }

  def bodyString(charset: Charset): CheckMaterializer[BodyStringCheckType, JmsCheck, Message, String] =
    new JmsCheckMaterializer(stringBodyPreparer(charset))

  def bodyBytes(charset: Charset): CheckMaterializer[BodyBytesCheckType, JmsCheck, Message, Array[Byte]] =
    new JmsCheckMaterializer(bodyBytesPreparer(charset))

  def bodyLength(charset: Charset): CheckMaterializer[BodyBytesCheckType, JmsCheck, Message, Int] =
    new JmsCheckMaterializer(bodyLengthPreparer(charset))

  def substring(charset: Charset): CheckMaterializer[SubstringCheckType, JmsCheck, Message, String] =
    new JmsCheckMaterializer(stringBodyPreparer(charset))

  def jmesPath(jsonParsers: JsonParsers, charset: Charset): CheckMaterializer[JmesPathCheckType, JmsCheck, Message, JsonNode] =
    new JmsCheckMaterializer(jsonPreparer(jsonParsers, charset))

  def jsonPath(jsonParsers: JsonParsers, charset: Charset): CheckMaterializer[JsonPathCheckType, JmsCheck, Message, JsonNode] =
    new JmsCheckMaterializer(jsonPreparer(jsonParsers, charset))

  val Xpath: CheckMaterializer[XPathCheckType, JmsCheck, Message, Option[XdmNode]] = {

    val errorMapper: String => String = "Could not parse response into a DOM Document: " + _

    val preparer: Preparer[Message, Option[XdmNode]] =
      message =>
        safely(errorMapper) {
          message match {
            case tm: TextMessage => Some(XmlParsers.parse(tm.getText)).success
            case _               => "Unsupported message type".failure
          }
        }

    new JmsCheckMaterializer(preparer)
  }
}
