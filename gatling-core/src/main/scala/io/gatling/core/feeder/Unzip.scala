/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import java.io.{ BufferedOutputStream, File, FileOutputStream, InputStream }
import java.util.zip.{ GZIPInputStream, ZipInputStream }

import scala.annotation.switch
import scala.util.Using

import io.gatling.commons.util.Io._
import io.gatling.core.util.{ FilesystemResource, Resource }

private[feeder] object Unzip {

  private object TwoBytesMagicValueInputStream {
    val PkZipMagicValue: (Int, Int) = ('P', 'K')
    val GzipMagicValue: (Int, Int) = (31, 139)
  }

  private final class TwoBytesMagicValueInputStream(is: InputStream) extends InputStream {
    val magicValue: (Int, Int) = (is.read(), is.read())
    private var pos: Int = 0

    override def read(): Int =
      (pos: @switch) match {
        case 0 =>
          pos += 1
          magicValue._1
        case 1 =>
          pos += 1
          magicValue._2
        case _ => is.read()
      }
  }

  def unzip(resource: Resource): Resource =
    unzip(resource.inputStream, resource.name)

  def unzip(inputStream: InputStream, name: String): Resource = {
    val tempFile = File.createTempFile(s"uncompressed-$name", null)
    tempFile.deleteOnExit()

    Using.resources(new BufferedOutputStream(new FileOutputStream(tempFile)), new TwoBytesMagicValueInputStream(inputStream)) { (os, is) =>
      is.magicValue match {
        case TwoBytesMagicValueInputStream.PkZipMagicValue =>
          val zis = new ZipInputStream(is)
          val zipEntry = zis.getNextEntry()
          if (zipEntry == null) {
            throw new IllegalArgumentException("ZIP Archive is empty")
          }

          zis.copyTo(os)

          val nextZipEntry = zis.getNextEntry()
          if (nextZipEntry != null) {
            throw new IllegalArgumentException(s"ZIP Archive contains more than one file (at least ${zipEntry.getName} and ${nextZipEntry.getName})")
          }

        case TwoBytesMagicValueInputStream.GzipMagicValue =>
          new GZIPInputStream(is).copyTo(os): Unit

        case _ => throw new IllegalArgumentException("Archive format not supported, couldn't find neither ZIP nor GZIP magic number")
      }

      FilesystemResource(tempFile)
    }
  }
}
