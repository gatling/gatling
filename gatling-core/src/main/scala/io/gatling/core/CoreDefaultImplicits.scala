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

import io.gatling.core.body.{ RawFileBodies, ElFileBodies }
import io.gatling.core.check.extractor.css.{ CssExtractorFactory, CssSelectors }
import io.gatling.core.check.extractor.jsonpath.{ JsonPathExtractorFactory, JsonPaths }
import io.gatling.core.check.extractor.regex.{ RegexExtractorFactory, Patterns }
import io.gatling.core.check.extractor.xpath.{ SaxonXPathExtractorFactory, Saxon, JdkXPathExtractorFactory, JdkXmlParsers }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers

trait CoreDefaultImplicits {

  implicit def configuration: GatlingConfiguration

  implicit lazy val defaultPatterns = new Patterns
  implicit lazy val defaultRegexExtractorFactory = new RegexExtractorFactory

  implicit lazy val defaultJsonParsers: JsonParsers = JsonParsers()
  implicit lazy val defaultJsonPaths = new JsonPaths
  implicit lazy val defaultJsonPathExtractorFactory = new JsonPathExtractorFactory

  implicit lazy val defaultJdkXmlParsers = new JdkXmlParsers
  implicit lazy val defaultJdkXPathExtractorFactory = new JdkXPathExtractorFactory

  implicit lazy val defaultSaxon = new Saxon
  implicit lazy val defaultSaxonXPathExtractorFactory = new SaxonXPathExtractorFactory

  implicit lazy val defaultCssSelectors = new CssSelectors
  implicit lazy val defaultCssExtractorFactory = new CssExtractorFactory

  implicit lazy val elFileBodies = new ElFileBodies
  implicit lazy val rawFileBodies = new RawFileBodies
}
