/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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

package io.gatling.http.check.async

import java.nio.charset.Charset

sealed trait AsyncMessage {
  def string: String
  def bytes: Array[Byte]
}

object StringAsyncMessage {

  def apply(string: String, charset: Charset) = {
    new StringAsyncMessage(string, charset)
  }
}

class StringAsyncMessage(val string: String, charset: Charset) extends AsyncMessage {
  lazy val bytes = string.getBytes(charset)
}

object ByteArrayAsyncMessage {

  def apply(bytes: Array[Byte], charset: Charset) = {
    new ByteArrayAsyncMessage(bytes, charset)
  }
}

class ByteArrayAsyncMessage(val bytes: Array[Byte], charset: Charset) extends AsyncMessage {
  lazy val string = new String(bytes, charset)
}
