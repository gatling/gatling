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

package io.gatling.commons.util

import java.{ lang => jl }

object Hex {

  def fromHexString(hexString: String): Array[Byte] =
    hexString.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray

  def toHexString(bytes: Array[Byte]): String = {
    val sb = new jl.StringBuilder(bytes.length)
    bytes.foreach { byte =>
      val shifted = byte & 0xff
      if (shifted < 0x10) {
        sb.append('0')
      }
      sb.append(jl.Long.toString(shifted.toLong, 16))
    }
    sb.toString
  }

  private val HexChars = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

  def toHexChar(digit: Int): Char = HexChars(digit)
}
