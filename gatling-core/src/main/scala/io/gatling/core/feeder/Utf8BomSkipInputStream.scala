/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.core.feeder

import java.io.InputStream

import scala.annotation.switch

private[feeder] object Utf8BomSkipInputStream {
  val Utf8BomByte1: Byte = 0xEF.asInstanceOf[Byte]
  val Utf8BomByte2: Byte = 0xBB.asInstanceOf[Byte]
  val Utf8BomByte3: Byte = 0xBF.asInstanceOf[Byte]
}

private[feeder] class Utf8BomSkipInputStream(is: InputStream) extends InputStream {

  import Utf8BomSkipInputStream._

  private val buffer = new Array[Byte](2)
  private var bufferPos = 0

  override def read(): Int =
    (bufferPos: @switch) match {
      case 0 =>
        val b1 = is.read().asInstanceOf[Byte]
        val b2 = is.read().asInstanceOf[Byte]
        val b3 = is.read().asInstanceOf[Byte]

        if (b1 == Utf8BomByte1 && b2 == Utf8BomByte2 && b3 == Utf8BomByte3) {
          bufferPos = 3
          is.read()
        } else {
          buffer(0) = b2
          buffer(1) = b3
          bufferPos = 1
          b1
        }
      case 1 =>
        bufferPos = 2
        buffer(0)

      case 2 =>
        bufferPos = 3
        buffer(1)

      case _ =>
        is.read()
    }
}
