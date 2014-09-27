/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.check.extractor.xpath

import io.gatling.core.check._
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.core.validation.Validation
import net.sf.saxon.s9api.XdmNode
import org.w3c.dom.Document
import org.xml.sax.InputSource

trait XPathCheckBuilder[C <: Check[R], R] {

  def preparer[T](f: InputSource => T)(response: R): Validation[Option[T]]

  val CheckBuilder: Extender[C, R]

  val SaxonXPathPreparer: Preparer[R, Option[XdmNode]] = preparer(SaxonXPathExtractor.parse)

  val JDKXPathPreparer: Preparer[R, Option[Document]] = preparer(JDKXPathExtractor.parse)

  def xpath(expression: Expression[String], namespaces: List[(String, String)]) =
    if (SaxonXPathExtractor.Enabled)
      new DefaultMultipleFindCheckBuilder[C, R, Option[XdmNode], String](CheckBuilder, SaxonXPathPreparer) {
        def findExtractor(occurrence: Int) = expression.map(new SaxonXPathExtractor.SingleXPathExtractor(_, namespaces, occurrence))
        def findAllExtractor = expression.map(new SaxonXPathExtractor.MultipleXPathExtractor(_, namespaces))
        def countExtractor = expression.map(new SaxonXPathExtractor.CountXPathExtractor(_, namespaces))
      }
    else
      new DefaultMultipleFindCheckBuilder[C, R, Option[Document], String](CheckBuilder, JDKXPathPreparer) {
        def findExtractor(occurrence: Int) = expression.map(new JDKXPathExtractor.SingleXPathExtractor(_, namespaces, occurrence))
        def findAllExtractor = expression.map(new JDKXPathExtractor.MultipleXPathExtractor(_, namespaces))
        def countExtractor = expression.map(new JDKXPathExtractor.CountXPathExtractor(_, namespaces))
      }
}
