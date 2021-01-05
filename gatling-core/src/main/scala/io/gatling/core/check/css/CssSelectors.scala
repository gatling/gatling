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

package io.gatling.core.check.css

import java.{ util => ju }

import scala.jdk.CollectionConverters._

import io.gatling.core.util.cache.Cache

import com.github.benmanes.caffeine.cache.LoadingCache
import jodd.csselly.{ CSSelly, CssSelector }
import jodd.lagarto.dom.NodeSelector

class CssSelectors(cacheMaxCapacity: Long) {

  private val domBuilder = Lagarto.newLagartoDomBuilder
  private val selectorCache: LoadingCache[String, ju.List[ju.List[CssSelector]]] =
    Cache.newConcurrentLoadingCache(cacheMaxCapacity, CSSelly.parse)

  def parse(chars: Array[Char]): NodeSelector = new NodeSelector(domBuilder.parse(chars))

  def extractAll[X: NodeConverter](selector: NodeSelector, criterion: (String, Option[String])): Vector[X] = {

    val (query, nodeAttribute) = criterion
    val selectors = selectorCache.get(query)

    selector
      .select(selectors)
      .asScala
      .view
      .flatMap(node => NodeConverter[X].convert(node, nodeAttribute).toList)
      .to(Vector)
  }
}
