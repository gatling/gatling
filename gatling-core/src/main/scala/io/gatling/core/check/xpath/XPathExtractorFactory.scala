/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.core.check.xpath

import scala.collection.JavaConverters._

import io.gatling.commons.validation._
import io.gatling.core.check._

import net.sf.saxon.s9api.XdmNode

class XPathFindExtractor(path: String, namespaces: Map[String, String], occurrence: Int, xmlParsers: XmlParsers)
    extends FindCriterionExtractor[Option[XdmNode], (String, Map[String, String]), String](
      "xpath",
      (path, namespaces),
      occurrence,
      _.flatMap { document =>
        val xdmValue = xmlParsers.evaluateXPath(path, namespaces, document)
        if (occurrence < xdmValue.size)
          Some(xdmValue.itemAt(occurrence).getStringValue)
        else
          None
      }.success
    )

class XPathFindAllExtractor(path: String, namespaces: Map[String, String], xmlParsers: XmlParsers)
    extends FindAllCriterionExtractor[Option[XdmNode], (String, Map[String, String]), String](
      "xpath",
      (path, namespaces),
      _.flatMap { document =>
        val xdmValue = xmlParsers.evaluateXPath(path, namespaces, document).asScala

        if (xdmValue.nonEmpty)
          // beware: we use toVector because xdmValue is an Iterable, so the Scala wrapper is a Stream
          // we don't want it to lazy load and hold a reference to the underlying DOM
          Some(xdmValue.map(_.getStringValue).toVector)
        else
          None
      }.success
    )

class XPathCountExtractor(path: String, namespaces: Map[String, String], xmlParsers: XmlParsers)
    extends CountCriterionExtractor[Option[XdmNode], (String, Map[String, String])](
      "xpath",
      (path, namespaces),
      _.map { document =>
        xmlParsers.evaluateXPath(path, namespaces, document).size
      }.orElse(Some(0)).success
    )
