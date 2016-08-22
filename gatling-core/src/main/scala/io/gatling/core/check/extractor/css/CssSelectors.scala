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
package io.gatling.core.check.extractor.css

import java.util.{ List => JList }

import scala.collection._
import scala.collection.JavaConversions.asScalaBuffer

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.cache.Cache

import com.github.benmanes.caffeine.cache.LoadingCache
import jodd.csselly.{ CSSelly, CssSelector }
import jodd.lagarto.dom.NodeSelector
import jodd.log.LoggerFactory
import jodd.log.impl.Slf4jLoggerFactory

class CssSelectors(implicit configuration: GatlingConfiguration) {

  LoggerFactory.setLoggerFactory(new Slf4jLoggerFactory)

  private val domBuilder = Jodd.newLagartoDomBuilder
  private val selectorCache: LoadingCache[String, JList[JList[CssSelector]]] =
    Cache.newConcurrentLoadingCache(configuration.core.extract.css.cacheMaxCapacity, CSSelly.parse)

  def parse(chars: Array[Char]) = new NodeSelector(domBuilder.parse(chars))

  def parse(string: String) = new NodeSelector(domBuilder.parse(string))

  def extractAll[X: NodeConverter](selector: NodeSelector, criterion: (String, Option[String])): Vector[X] = {

    val (query, nodeAttribute) = criterion
    val selectors = selectorCache.get(query)

    selector.select(selectors).flatMap { node =>
      NodeConverter[X].convert(node, nodeAttribute)
    }(breakOut)
  }
}
