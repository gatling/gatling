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

package io.gatling.jms

import javax.jms.{ BytesMessage, Message, TextMessage }

import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar

trait MockMessage extends MockitoSugar {

  def textMessage(text: String): TextMessage = {
    val msg = mock[TextMessage]
    when(msg.getText) thenReturn text
    msg
  }

  def bytesMessage(bytes: Array[Byte]): BytesMessage =
    mock[BytesMessage](new BytesMessageAnswer(bytes))

  def message: Message = mock[Message]
}
