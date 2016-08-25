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
package io.gatling.core

import io.gatling.core.body.{ ElFileBodies, RawFileBodies }
import io.gatling.core.check.extractor.css.CssSelectors
import io.gatling.core.check.extractor.jsonpath.{ JsonPathExtractorFactory, JsonPaths }
import io.gatling.core.check.extractor.regex.{ Patterns, RegexExtractorFactory }
import io.gatling.core.check.extractor.xpath.{ JdkXmlParsers, Saxon }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers

trait CoreDefaultImplicits {

  implicit def configuration: GatlingConfiguration

  implicit lazy val defaultPatterns = new Patterns

  implicit lazy val defaultJsonParsers: JsonParsers = JsonParsers()
  implicit lazy val defaultJsonPaths = new JsonPaths

  implicit lazy val defaultSaxon = new Saxon
  implicit lazy val defaultJdkXmlParsers = new JdkXmlParsers

  implicit lazy val defaultCssSelectors = new CssSelectors

  implicit lazy val elFileBodies = new ElFileBodies
  implicit lazy val rawFileBodies = new RawFileBodies

  @deprecated("Only used in old Async checks, will be replaced with new impl, will be removed in 3.0.0", "3.0.0-M1")
  implicit lazy val defaultRegexExtractorFactory = new RegexExtractorFactory(defaultPatterns)
  @deprecated("Only used in old Async checks, will be replaced with new impl, will be removed in 3.0.0", "3.0.0-M1")
  implicit lazy val defaultJsonPathExtractorFactory = new JsonPathExtractorFactory(defaultJsonPaths)
}
