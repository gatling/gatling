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

import java.io._
import java.net.{ URISyntaxException, URL }
import java.nio.charset.Charset

import scala.io.Source

object Io {

  val DefaultBufferSize: Int = 8 * 1024

  @deprecated("Will be removed once FrontLine stop supporting Gatling 3.4", "3.5.0")
  implicit class RichURL(val url: URL) extends AnyVal {

    def file: File =
      try {
        new File(url.toURI)
      } catch {
        case _: URISyntaxException => new File(url.getPath)
      }
  }

  // FIXME drop when switching to Java 9+
  implicit class RichInputStream(val is: InputStream) extends AnyVal {

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    // FIXME only used for tests and Resource#string (not on hot path): replace with new String(InputStream#readAllBytes) (Java 9+)
    def toString(charset: Charset, bufferSize: Int = DefaultBufferSize): String = {
      val reader = new InputStreamReader(is, charset)
      val buffer = new Array[Char](bufferSize)

      var lastReadCount: Int = 0
      def read(): Int = {
        lastReadCount = reader.read(buffer)
        lastReadCount
      }

      val writer = new FastStringWriter(bufferSize)
      while (read() != -1) {
        writer.write(buffer, 0, lastReadCount)
      }

      writer.toString
    }

    // FIXME https://docs.oracle.com/javase/9/docs/api/java/io/InputStream.html#readAllBytes-- (Java 9+)
    def toByteArray(): Array[Byte] = {
      val os = FastByteArrayOutputStream.pooled()
      os.write(is)
      os.toByteArray
    }

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
    // FIXME https://docs.oracle.com/javase/9/docs/api/java/io/InputStream.html#transferTo-java.io.OutputStream- (Java 9+)
    def copyTo(os: OutputStream, bufferSize: Int = DefaultBufferSize): Int = {

      def copyLarge(buffer: Array[Byte]): Long = {

        var lastReadCount: Int = 0
        def read(): Int = {
          lastReadCount = is.read(buffer)
          lastReadCount
        }

        var count: Long = 0

        while (read() != -1) {
          os.write(buffer, 0, lastReadCount)
          count += lastReadCount
        }

        count
      }

      copyLarge(new Array[Byte](bufferSize)) match {
        case l if l > Integer.MAX_VALUE => -1
        case l                          => l.toInt
      }
    }
  }

  @deprecated("Use scala.util.Using", "3.5.0")
  def withCloseable[T, C <: AutoCloseable](closeable: C)(block: C => T): T =
    try block(closeable)
    finally closeable.close()

  @deprecated("Use scala.util.Using", "3.5.0")
  def withSource[T, C <: Source](closeable: C)(block: C => T): T =
    try block(closeable)
    finally closeable.close()
}
