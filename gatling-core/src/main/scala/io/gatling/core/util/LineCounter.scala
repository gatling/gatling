/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.core.util

import java.io._
import java.nio.charset.Charset

import scala.util.Using

import io.github.metarank.cfor._

object LineCounter {

  val DefaultBufferSize: Int = 8 * 1024
  def apply(charset: Charset): LineCounter = new LineCounter(charset, DefaultBufferSize)
}

class LineCounter(charset: Charset, bufferSize: Int) {
  protected var lines = 0
  private var inLine = false

  private var read = 0
  private val readCharBuffer = new Array[Char](bufferSize)

  protected def continueReading: Boolean = true

  protected def onChar(c: Char): Unit = {}

  protected def onLine(): Unit = {}

  protected def onDone(): Unit = {}

  def countLines(is: InputStream): Int =
    Using.resource(new InputStreamReader(new BufferedInputStream(is, bufferSize), charset)) { reader =>
      while (read != -1 && continueReading) {
        cfor(0 until read) { i =>
          val c = readCharBuffer(i)
          if (c == '\r' || c == '\n') {
            if (inLine) {
              lines += 1
              onLine()
            }
            inLine = false
          } else {
            inLine = true
            onChar(c)
          }
        }
        read = reader.read(readCharBuffer)
      }

      // last line
      if (inLine) {
        lines += 1
      }
      onDone()
      val res = lines

      // reset
      lines = 0
      inLine = false
      read = 0

      res
    }
}
