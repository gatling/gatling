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

package io.gatling.core.feeder

import java.io.{ File, FileOutputStream }
import java.util.zip.{ GZIPInputStream, ZipInputStream }

import io.gatling.commons.util.Io._
import io.gatling.core.util._
import io.gatling.commons.util.Io.withCloseable
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.Resource

import com.typesafe.scalalogging.LazyLogging

sealed trait FeederSource[T] {
  def feeder(options: FeederOptions[T], configuration: GatlingConfiguration): Feeder[Any]
}

case class InMemoryFeederSource[T](records: IndexedSeq[Record[T]]) extends FeederSource[T] with LazyLogging {

  require(records.nonEmpty, "Feeder must not be empty")

  override def feeder(options: FeederOptions[T], configuration: GatlingConfiguration): Feeder[Any] = {

    val rawRecords: IndexedSeq[Record[T]] =
      configuration.resolve(
        // [fl]
        //
        //
        //
        //
        //
        //
        //
        //
        // [fl]
        {
          if (options.shard) {
            logger.warn("shard is an option that's only supported in FrontLine")
          }
          records
        }
      )

    InMemoryFeeder(rawRecords, options.conversion, options.strategy)
  }
}

object SeparatedValuesFeederSource {
  private val BufferSize = 1024

  def unzip(resource: Resource): Resource = {
    val tempFile = File.createTempFile(s"uncompressed-${resource.name}", null)
    tempFile.deleteOnExit()

    val magicNumber: (Int, Int) = withCloseable(resource.inputStream) { os =>
      (os.read(), os.read())
    }

    magicNumber match {
      case ('P', 'K') => // PK: zip
        withCloseable(new ZipInputStream(resource.inputStream)) { zis =>
          try {
            val zipEntry = zis.getNextEntry()
            if (zipEntry == null) {
              throw new IllegalArgumentException("ZIP Archive is empty")
            }

            withCloseable(new FileOutputStream(tempFile)) { os =>
              zis.copyTo(os, BufferSize)
            }

            if (zis.getNextEntry() != null) {
              throw new IllegalArgumentException("ZIP Archive contains more than one file")
            }

          } finally {
            zis.closeEntry()
          }
        }

      case (31, 139) => // gzip
        withCloseable(new GZIPInputStream(resource.inputStream, BufferSize)) { is =>
          withCloseable(new FileOutputStream(tempFile)) { os =>
            is.copyTo(os, BufferSize)
          }
        }

      case _ => throw new IllegalArgumentException("Archive format not supported, couldn't find neither ZIP nor GZIP magic number")
    }

    FilesystemResource(tempFile)
  }
}

class SeparatedValuesFeederSource(resource: Resource, separator: Char, quoteChar: Char) extends FeederSource[String] {

  override def feeder(options: FeederOptions[String], configuration: GatlingConfiguration): Feeder[Any] = {
    val charset = configuration.core.charset

    val uncompressedResource =
      if (options.unzip) {
        SeparatedValuesFeederSource.unzip(resource)
      } else {
        resource
      }

    def applyBatch(resource: Resource): Feeder[Any] =
      options.loadingMode match {
        case Batch(bufferSize) =>
          BatchedSeparatedValuesFeeder(resource.file, separator, quoteChar, options.conversion, options.strategy, bufferSize, charset)
        case Adaptive if resource.file.length > configuration.core.feederAdaptiveLoadModeThreshold =>
          BatchedSeparatedValuesFeeder(resource.file, separator, quoteChar, options.conversion, options.strategy, Batch.DefaultBufferSize, charset)
        case _ =>
          val records = SeparatedValuesParser.parse(resource, separator, quoteChar, charset)
          InMemoryFeeder(records, options.conversion, options.strategy)
      }

    configuration.resolve(
      // [fl]
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      //
      // [fl]
      applyBatch(uncompressedResource)
    )
  }
}
