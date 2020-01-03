/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.validation._
import io.gatling.core.check.Preparer
import io.gatling.core.json.JsonParsers

import com.fasterxml.jackson.databind.JsonNode
import javax.jms.{ BytesMessage, Message, TextMessage }

object JmsMessageBodyPreparers {

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

  private[check] def jmsStringBodyPreparer(charset: Charset): Preparer[Message, String] = {
    case tm: TextMessage  => tm.getText.success
    case bm: BytesMessage => getBodyAsString(bm, charset).success
    case _                => "Unsupported message type".failure
  }

  private[check] def jmsBytesBodyPreparer(charset: Charset): Preparer[Message, Array[Byte]] = {
    case tm: TextMessage  => tm.getText.getBytes(charset).success
    case bm: BytesMessage => toBytes(bm).success
    case _                => "Unsupported message type".failure
  }

  private val JmsJsonPreparerErrorMapper: String => String = "Could not parse response into a JSON: " + _

  private[check] def jmsJsonPreparer(jsonParsers: JsonParsers, charset: Charset): Preparer[Message, JsonNode] =
    replyMessage =>
      safely(JmsJsonPreparerErrorMapper) {
        replyMessage match {
          case tm: TextMessage  => jsonParsers.safeParse(tm.getText)
          case bm: BytesMessage => jsonParsers.safeParse(getBodyAsString(bm, charset))
          case _                => "Unsupported message type".failure
        }
      }
}
