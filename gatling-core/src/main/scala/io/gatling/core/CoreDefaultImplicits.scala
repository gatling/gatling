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

package io.gatling.core

import io.gatling.core.body.{ ElFileBodies, PebbleFileBodies, RawFileBodies }
import io.gatling.core.check.css.CssSelectors
import io.gatling.core.check.jmespath.JmesPaths
import io.gatling.core.check.jsonpath.JsonPaths
import io.gatling.core.check.regex.Patterns
import io.gatling.core.check.xpath.XmlParsers
import io.gatling.core.config.{ GatlingConfiguration, GatlingFiles }
import io.gatling.core.json.JsonParsers

trait CoreDefaultImplicits {

  implicit def configuration: GatlingConfiguration

  lazy implicit val defaultPatterns: Patterns = new Patterns(configuration.core.extract.regex.cacheMaxCapacity)
  lazy implicit val defaultJsonParsers: JsonParsers = new JsonParsers
  lazy implicit val defaultJsonPaths: JsonPaths = new JsonPaths(configuration.core.extract.jsonPath.cacheMaxCapacity)
  lazy implicit val defaultJmesPaths: JmesPaths = new JmesPaths(configuration.core.extract.jsonPath.cacheMaxCapacity)
  lazy implicit val defaultXmlParsers: XmlParsers = new XmlParsers(configuration.core.extract.xpath.cacheMaxCapacity)
  lazy implicit val defaultCssSelectors: CssSelectors = new CssSelectors(configuration.core.extract.css.cacheMaxCapacity)
  lazy implicit val elFileBodies: ElFileBodies =
    new ElFileBodies(GatlingFiles.resourcesDirectory(configuration), configuration.core.charset, configuration.core.elFileBodiesCacheMaxCapacity)
  lazy implicit val rawFileBodies: RawFileBodies =
    new RawFileBodies(GatlingFiles.resourcesDirectory(configuration), configuration.core.rawFileBodiesInMemoryMaxSize)
  lazy implicit val pebbleFileBodies: PebbleFileBodies =
    new PebbleFileBodies(GatlingFiles.resourcesDirectory(configuration), configuration.core.pebbleFileBodiesCacheMaxCapacity)
}
