/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.core.body

import java.io.File

import io.gatling.commons.util.Io._
import io.gatling.commons.validation._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Expression
import io.gatling.core.util.Resource
import io.gatling.core.util.cache.Cache

import com.github.benmanes.caffeine.cache.LoadingCache

case class FileWithCachedBytes(file: File, cachedBytes: Option[Array[Byte]]) {
  def bytes: Array[Byte] = cachedBytes.getOrElse(file.toByteArray)
}

class RawFileBodies(implicit configuration: GatlingConfiguration) {

  private val rawFileBodyCache: LoadingCache[String, Validation[File]] = {
    val pathToFile: String => Validation[File] = path => Resource.body(path).map(_.file)
    Cache.newConcurrentLoadingCache(configuration.core.rawFileBodiesCacheMaxCapacity, pathToFile)
  }

  private val rawFileBodyBytesCache: LoadingCache[String, Validation[Array[Byte]]] = {
    val pathToFileBytes: String => Validation[Array[Byte]] = path => Resource.body(path).map(_.file.toByteArray)
    Cache.newConcurrentLoadingCache(configuration.core.rawFileBodiesCacheMaxCapacity, pathToFileBytes)
  }

  private def cachedBytes(file: File): Validation[Option[Array[Byte]]] =
    if (file.length > configuration.core.rawFileBodiesInMemoryMaxSize)
      Success(None)
    else
      rawFileBodyBytesCache.get(file.getPath).map(Some(_))

  def asFileWithCachedBytes(filePath: Expression[String]): Expression[FileWithCachedBytes] =
    session =>
      for {
        path <- filePath(session)
        file <- rawFileBodyCache.get(path)
        validatedFile <- file.validateExistingReadable
        cachedBytes <- cachedBytes(validatedFile)
      } yield FileWithCachedBytes(validatedFile, cachedBytes)
}
