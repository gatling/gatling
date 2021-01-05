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

package io.gatling.core.feeder

import java.io.{ BufferedOutputStream, File, FileOutputStream, InputStream }
import java.nio.channels.FileChannel
import java.util.zip.{ GZIPInputStream, ZipInputStream }

import scala.annotation.switch
import scala.util.Using

import io.gatling.commons.util.Io._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util._

import com.typesafe.scalalogging.LazyLogging

sealed trait FeederSource[T] {
  def feeder(options: FeederOptions[T], configuration: GatlingConfiguration): Feeder[Any]
}

final case class InMemoryFeederSource[T](records: IndexedSeq[Record[T]]) extends FeederSource[T] with LazyLogging {

  require(records.nonEmpty, "Feeder must not be empty")

  override def feeder(options: FeederOptions[T], configuration: GatlingConfiguration): Feeder[Any] =
    InMemoryFeeder(records, options.conversion, options.strategy)
}

private object TwoBytesMagicValueInputStream {
  val PkZipMagicValue: (Int, Int) = ('P', 'K')
  val GzipMagicValue: (Int, Int) = (31, 139)
}

private class TwoBytesMagicValueInputStream(is: InputStream) extends InputStream {
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

object SeparatedValuesFeederSource {

  private def unzip(resource: Resource): Resource = {
    val tempFile = File.createTempFile(s"uncompressed-${resource.name}", null)
    tempFile.deleteOnExit()

    Using.resources(new BufferedOutputStream(new FileOutputStream(tempFile)), new TwoBytesMagicValueInputStream(resource.inputStream)) { (os, is) =>
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

final class SeparatedValuesFeederSource(resource: Resource, separator: Char, quoteChar: Char) extends FeederSource[String] {

  override def feeder(options: FeederOptions[String], configuration: GatlingConfiguration): Feeder[Any] = {

    val uncompressedResource =
      if (options.unzip) {
        SeparatedValuesFeederSource.unzip(resource)
      } else {
        resource
      }

    def applyBatch(res: Resource): Feeder[Any] = {
      val charset = configuration.core.charset
      options.loadingMode match {
        case Batch(bufferSize) =>
          BatchedSeparatedValuesFeeder(res.file, separator, quoteChar, options.conversion, options.strategy, bufferSize, charset)
        case Adaptive if res.file.length > configuration.core.feederAdaptiveLoadModeThreshold =>
          BatchedSeparatedValuesFeeder(res.file, separator, quoteChar, options.conversion, options.strategy, Batch.DefaultBufferSize, charset)
        case _ =>
          val records = Using.resource(FileChannel.open(res.file.toPath)) { channel =>
            SeparatedValuesParser.stream(separator, quoteChar, charset)(channel).toVector
          }

          InMemoryFeeder(records, options.conversion, options.strategy)
      }
    }

    applyBatch(uncompressedResource)
  }
}
