/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.core.check.extractor.xpath

import io.gatling.core.check.DefaultMultipleFindCheckBuilder
import io.gatling.core.check.extractor.Extractor
import io.gatling.core.session._

import net.sf.saxon.s9api.XdmNode
import org.w3c.dom.Document

trait XPathCheckType

sealed trait Dom
final case class SaxonDom(document: XdmNode) extends Dom
final case class JdkDom(document: Document) extends Dom

class XPathCheckBuilder(
    path:       Expression[String],
    namespaces: List[(String, String)],
    xmlParsers: XmlParsers
)
  extends DefaultMultipleFindCheckBuilder[XPathCheckType, Option[Dom], String](displayActualValue = true) {

  override def findExtractor(occurrence: Int): Expression[Extractor[Option[Dom], String]] = path.map(new XPathFindExtractor(_, namespaces, occurrence, xmlParsers))
  override def findAllExtractor: Expression[Extractor[Option[Dom], Seq[String]]] = path.map(new XPathFindAllExtractor(_, namespaces, xmlParsers))
  override def countExtractor: Expression[Extractor[Option[Dom], Int]] = path.map(new XPathCountExtractor(_, namespaces, xmlParsers))
}
