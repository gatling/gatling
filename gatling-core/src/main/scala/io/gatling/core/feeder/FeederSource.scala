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

package io.gatling.core.feeder

import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap

import scala.jdk.CollectionConverters._
import scala.util.Using

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.{ Json, JsonParsers }
import io.gatling.core.util._

import com.typesafe.scalalogging.LazyLogging

private[gatling] sealed trait FeederSource[T] {
  def feeder(options: FeederOptions[T], configuration: GatlingConfiguration): Feeder[Any]

  def name: String

  def recordsCount(options: FeederOptions[T], configuration: GatlingConfiguration): Int
}

private[gatling] final case class InMemoryFeederSource[T](records: IndexedSeq[Record[T]], override val name: String) extends FeederSource[T] with LazyLogging {
  require(records.nonEmpty, "Feeder must not be empty")

  override def feeder(options: FeederOptions[T], configuration: GatlingConfiguration): Feeder[Any] =
    InMemoryFeeder(records, options.conversion, options.strategy)

  override def recordsCount(options: FeederOptions[T], configuration: GatlingConfiguration): Int =
    records.length
}

private[feeder] object ZippedResourceCache {
  private val cache = new ConcurrentHashMap[Resource, Resource]()

  def unzipped(rawResource: Resource, unzip: Boolean): Resource =
    if (unzip) cache.computeIfAbsent(rawResource, Unzip.unzip) else rawResource
}

private[gatling] final class JsonFileFeederSource(resource: Resource, jsonParsers: JsonParsers) extends FeederSource[Any] {

  override def feeder(options: FeederOptions[Any], configuration: GatlingConfiguration): Feeder[Any] =
    Using.resource(ZippedResourceCache.unzipped(resource, options.unzip).inputStream) { is =>
      val node = jsonParsers.parse(is)
      require(node.isArray, "Root element of JSON feeder file isn't an array")

      val records = node.elements.asScala.collect {
        case node if node.isObject => Json.asScala(node).asInstanceOf[collection.immutable.Map[String, Any]]
      }.toVector
      InMemoryFeeder(records, options.conversion, options.strategy)
    }

  override def name: String = s"json(${resource.name})"

  override def recordsCount(options: FeederOptions[Any], configuration: GatlingConfiguration): Int =
    Using.resource(ZippedResourceCache.unzipped(resource, options.unzip).inputStream) { is =>
      Json.arrayOfObjectsLength(is, jsonParsers)
    }
}

private[gatling] final class SeparatedValuesFeederSource(val resource: Resource, separator: Char, quoteChar: Char) extends FeederSource[String] {
  override def feeder(options: FeederOptions[String], configuration: GatlingConfiguration): Feeder[Any] = {
    def applyBatch(res: Resource): Feeder[Any] = {
      val charset = configuration.core.charset
      options.loadingMode match {
        case Batch(bufferSize) =>
          BatchedSeparatedValuesFeeder(res.file, separator, quoteChar, options.conversion, options.strategy, bufferSize, charset)
        case Adaptive if res.file.length > configuration.core.feederAdaptiveLoadModeThreshold =>
          BatchedSeparatedValuesFeeder(res.file, separator, quoteChar, options.conversion, options.strategy, Batch.DefaultBufferLines, charset)
        case _ =>
          val records = Using.resource(FileChannel.open(res.file.toPath)) { channel =>
            SeparatedValuesParser.feederFactory(separator, quoteChar, charset)(channel).toVector
          }

          InMemoryFeeder(records, options.conversion, options.strategy)
      }
    }

    val uncompressedResource = ZippedResourceCache.unzipped(resource, options.unzip)
    applyBatch(uncompressedResource)
  }

  override def recordsCount(options: FeederOptions[String], configuration: GatlingConfiguration): Int = {
    val uncompressedResource = ZippedResourceCache.unzipped(resource, options.unzip)
    val linesIncludingHeader = Using.resource(uncompressedResource.inputStream)(LineCounter(configuration.core.charset).countLines)

    linesIncludingHeader - 1
  }

  override def name: String = s"csv(${resource.name})"
}
