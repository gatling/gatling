/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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
import io.gatling.core.check.extractor.css.CssSelectors
import io.gatling.core.check.extractor.jsonpath.JsonPaths
import io.gatling.core.check.extractor.regex.Patterns
import io.gatling.core.check.extractor.xpath.XmlParsers
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers

trait CoreDefaultImplicits {

  implicit def configuration: GatlingConfiguration

  implicit lazy val defaultPatterns: Patterns = new Patterns

  implicit lazy val defaultJsonParsers: JsonParsers = JsonParsers()
  implicit lazy val defaultJsonPaths: JsonPaths = new JsonPaths

  implicit lazy val defaultXmlParsers: XmlParsers = new XmlParsers

  implicit lazy val defaultCssSelectors: CssSelectors = new CssSelectors

  implicit lazy val elFileBodies: ElFileBodies = new ElFileBodies
  implicit lazy val rawFileBodies: RawFileBodies = new RawFileBodies
  implicit lazy val pebbleFileBodies: PebbleFileBodies = new PebbleFileBodies
}
