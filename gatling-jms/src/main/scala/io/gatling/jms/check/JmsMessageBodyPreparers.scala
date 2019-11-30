/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import java.nio.charset.StandardCharsets

import io.gatling.commons.validation._
import io.gatling.core.check.Preparer
import io.gatling.core.config.GatlingConfiguration

import javax.jms.{ BytesMessage, Message, TextMessage }

object JmsMessageBodyPreparers {

  private def toBytes(bytesMessage: BytesMessage): Array[Byte] = {
    val buffer = Array.ofDim[Byte](bytesMessage.getBodyLength.toInt)
    bytesMessage.readBytes(buffer)
    buffer
  }

  protected[check] def getBodyAsString(bytesMessage: BytesMessage, config: GatlingConfiguration): String =
    if (config.core.charset == StandardCharsets.UTF_8)
      bytesMessage.readUTF()
    else
      new String(toBytes(bytesMessage), config.core.charset)

  def jmsStringBodyPreparer(config: GatlingConfiguration): Preparer[Message, String] = {
    case tm: TextMessage  => tm.getText.success
    case bm: BytesMessage => getBodyAsString(bm, config).success
    case _                => "Unsupported message type".failure
  }

  def jmsBytesBodyPreparer(config: GatlingConfiguration): Preparer[Message, Array[Byte]] = {
    case tm: TextMessage  => tm.getText.getBytes(config.core.charset).success
    case bm: BytesMessage => toBytes(bm).success
    case _                => "Unsupported message type".failure
  }

}
