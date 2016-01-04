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
package io.gatling.core.check.extractor.xpath

import io.gatling.commons.validation.Validation
import io.gatling.core.check._
import io.gatling.core.session.{ Expression, RichExpression }
import net.sf.saxon.s9api.XdmNode
import org.w3c.dom.Document
import org.xml.sax.InputSource

trait XPathCheckBuilder[C <: Check[R], R] {

  def preparer[T](f: InputSource => T)(response: R): Validation[Option[T]]

  val CheckBuilder: Extender[C, R]

  def saxonXPathPreparer(saxon: Saxon): Preparer[R, Option[XdmNode]] = preparer(saxon.parse)

  def jdkXPathPreparer(jdkXPath: JdkXmlParsers): Preparer[R, Option[Document]] = preparer(jdkXPath.parse)

  def xpath(expression: Expression[String], namespaces: List[(String, String)])(implicit saxonXPathExtractorFactory: SaxonXPathExtractorFactory, jdkXPathExtractorFactory: JdkXPathExtractorFactory) =
    if (saxonXPathExtractorFactory.saxon.enabled) {

      import saxonXPathExtractorFactory._

      new DefaultMultipleFindCheckBuilder[C, R, Option[XdmNode], String](CheckBuilder, saxonXPathPreparer(saxon)) {
        def findExtractor(occurrence: Int) = expression.map(path => newSingleExtractor((path, namespaces), occurrence))
        def findAllExtractor = expression.map(newMultipleExtractor(_, namespaces))
        def countExtractor = expression.map(newCountExtractor(_, namespaces))
      }
    } else {

      import jdkXPathExtractorFactory._

      new DefaultMultipleFindCheckBuilder[C, R, Option[Document], String](CheckBuilder, jdkXPathPreparer(jdkXmlParsers)) {
        def findExtractor(occurrence: Int) = expression.map(path => newSingleExtractor((path, namespaces), occurrence))
        def findAllExtractor = expression.map(newMultipleExtractor(_, namespaces))
        def countExtractor = expression.map(newCountExtractor(_, namespaces))
      }
    }
}
