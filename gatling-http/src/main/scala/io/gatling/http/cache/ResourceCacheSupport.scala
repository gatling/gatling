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

package io.gatling.http.cache

import java.util.concurrent.ConcurrentMap

import scala.compat.java8.FunctionConverters._

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.cache.Cache
import io.gatling.http.client.uri.Uri
import io.gatling.http.fetch.ConcurrentResource
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.request.HttpRequest

private[cache] final case class InferredPageResources(expire: String, requests: List[HttpRequest])
private[cache] final case class InferredResourcesCacheKey(protocol: HttpProtocol, uri: Uri)

private[cache] trait ResourceCacheSupport {

  def configuration: GatlingConfiguration

  // FIXME should CssContentCache use the same key?
  private val cssContentCache: ConcurrentMap[Uri, List[ConcurrentResource]] =
    Cache.newConcurrentCache[Uri, List[ConcurrentResource]](configuration.http.fetchedCssCacheMaxCapacity)
  private val inferredResourcesCache: ConcurrentMap[InferredResourcesCacheKey, InferredPageResources] =
    Cache.newConcurrentCache[InferredResourcesCacheKey, InferredPageResources](configuration.http.fetchedHtmlCacheMaxCapacity)

  def isCssCached(uri: Uri): Boolean = cssContentCache.get(uri) != null

  def computeCssResourcesIfAbsent(uri: Uri, f: Uri => List[ConcurrentResource]): List[ConcurrentResource] =
    cssContentCache.computeIfAbsent(uri, f.asJava)

  def removeCssResources(uri: Uri): Unit =
    cssContentCache.remove(uri)

  def getCachedInferredResources(httpProtocol: HttpProtocol, htmlDocumentUri: Uri): InferredPageResources =
    inferredResourcesCache.get(InferredResourcesCacheKey(httpProtocol, htmlDocumentUri))

  def computeInferredResourcesIfAbsent(
      httpProtocol: HttpProtocol,
      uri: Uri,
      lastModifiedOrEtag: String,
      computeResources: () => List[HttpRequest]
  ): List[HttpRequest] = {
    val cacheKey = InferredResourcesCacheKey(httpProtocol, uri)
    Option(inferredResourcesCache.get(cacheKey)) match {
      case Some(InferredPageResources(`lastModifiedOrEtag`, inferredResources)) =>
        //cache entry didn't expire, use it
        inferredResources
      case _ =>
        // cache entry missing or expired, update it
        val inferredResources = computeResources()
        inferredResourcesCache.put(cacheKey, InferredPageResources(lastModifiedOrEtag, inferredResources))
        inferredResources
    }
  }
}
