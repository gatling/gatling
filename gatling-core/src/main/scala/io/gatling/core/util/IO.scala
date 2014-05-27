/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.util

import java.io.{ File => JFile, _ }
import java.net.{ URLConnection, URISyntaxException, URL }
import scala.io.Source
import scala.util.Try

import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import java.nio.charset.Charset

object IO {

  val DefaultBufferSize = 4 * 1024

  implicit class RichURL(val url: URL) extends AnyVal {

    def jfile: JFile = Try(new JFile(url.toURI))
      .recover { case e: URISyntaxException => new JFile(url.getPath) }
      .get

    def toByteArray(bufferSize: Int = DefaultBufferSize): Array[Byte] =
      withCloseable(url.openConnection.getInputStream) {
        _.toByteArray(bufferSize)
      }
  }

  implicit class RichFile(val file: JFile) extends AnyVal {

    def validateExistingReadable: Validation[JFile] =
      if (!file.exists)
        s"File $file doesn't exist".failure
      else if (!file.canRead)
        s"File $file can't be read".failure
      else
        file.success

    def toByteArray(bufferSize: Int = DefaultBufferSize): Array[Byte] =
      withCloseable(new FileInputStream(file)) {
        _.toByteArray(bufferSize)
      }

    def toString(charset: Charset, bufferSize: Int = DefaultBufferSize): String =
      withCloseable(new FileInputStream(file)) {
        _.toString(charset, bufferSize)
      }
  }

  implicit class RichInputStream(val is: InputStream) extends AnyVal {

    def toString(charset: Charset, bufferSize: Int = DefaultBufferSize): String = {
      val writer = new UnsyncStringWriter(bufferSize)
      val reader = new InputStreamReader(is, charset)

      reader.copyTo(writer, bufferSize)

      writer.toString
    }

    def toCharArray(charset: Charset, bufferSize: Int = DefaultBufferSize): Array[Char] = {
      val writer = new UnsyncCharArrayWriter
      val reader = new InputStreamReader(is, charset)

      reader.copyTo(writer, bufferSize)

      writer.toCharArray
    }

    def toByteArray(bufferSize: Int = DefaultBufferSize): Array[Byte] = {

      val os = new ByteArrayOutputStream()
      is.copyTo(os, bufferSize)

      os.toByteArray
    }

    def copyTo(os: OutputStream, bufferSize: Int = DefaultBufferSize): Int = {

        def copyLarge(buffer: Array[Byte]) = {

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

  implicit class RichReader(val reader: Reader) extends AnyVal {

    def copyTo(writer: Writer, bufferSize: Int = DefaultBufferSize): Int = {

        def copyLarge(buffer: Array[Char]) = {

          var lastReadCount: Int = 0
            def read(): Int = {
              lastReadCount = reader.read(buffer)
              lastReadCount
            }

          var count: Long = 0

          while (read() != -1) {
            writer.write(buffer, 0, lastReadCount)
            count += lastReadCount
          }

          count
        }

      copyLarge(new Array[Char](bufferSize)) match {
        case l if l > Integer.MAX_VALUE => -1
        case l                          => l.toInt
      }

      copyLarge(new Array[Char](bufferSize)) match {
        case l if l > Integer.MAX_VALUE => -1
        case l                          => l.toInt
      }
    }
  }

  def withCloseable[T, C <: Closeable](closeable: C)(block: C => T) = {
    try
      block(closeable)
    finally
      closeable.close()
  }

  def withSource[T, C <: Source](closeable: C)(block: C => T) = {
    try
      block(closeable)
    finally
      closeable.close()
  }
}
