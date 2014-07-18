/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.check.body

import java.io.InputStream

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.check.{ DefaultMultipleFindCheckBuilder, Preparer }
import io.gatling.core.check.extractor.xpath.{ JDKXPathExtractor, SaxonXPathExtractor }
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.core.validation._
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response
import net.sf.saxon.s9api.XdmNode
import org.w3c.dom.Document

object HttpBodyXPathCheckBuilder extends StrictLogging {

  def preparer[T](f: InputStream => T)(response: Response): Validation[Option[T]] =
    try {
      val root = if (response.hasResponseBody) Some(f(response.body.stream)) else None
      root.success

    } catch {
      case e: Exception =>
        val message = s"Could not parse response into a DOM Document: ${e.getMessage}"
        logger.info(message, e)
        message.failure
    }

  val SaxonXPathPreparer: Preparer[Response, Option[XdmNode]] = preparer(SaxonXPathExtractor.parse)

  val JDKXPathPreparer: Preparer[Response, Option[Document]] = preparer(JDKXPathExtractor.parse)

  def xpath(expression: Expression[String], namespaces: List[(String, String)]) =
    if (SaxonXPathExtractor.Enabled)
      new DefaultMultipleFindCheckBuilder[HttpCheck, Response, Option[XdmNode], String](StreamBodyCheckFactory, SaxonXPathPreparer) {
        def findExtractor(occurrence: Int) = expression.map(new SaxonXPathExtractor.SingleXPathExtractor(_, namespaces, occurrence))
        def findAllExtractor = expression.map(new SaxonXPathExtractor.MultipleXPathExtractor(_, namespaces))
        def countExtractor = expression.map(new SaxonXPathExtractor.CountXPathExtractor(_, namespaces))
      }
    else
      new DefaultMultipleFindCheckBuilder[HttpCheck, Response, Option[Document], String](StreamBodyCheckFactory, JDKXPathPreparer) {
        def findExtractor(occurrence: Int) = expression.map(new JDKXPathExtractor.SingleXPathExtractor(_, namespaces, occurrence))
        def findAllExtractor = expression.map(new JDKXPathExtractor.MultipleXPathExtractor(_, namespaces))
        def countExtractor = expression.map(new JDKXPathExtractor.CountXPathExtractor(_, namespaces))
      }
}
