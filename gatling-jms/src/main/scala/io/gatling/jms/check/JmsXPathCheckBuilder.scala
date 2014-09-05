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
package io.gatling.jms.check

import java.io.StringReader
import javax.jms.{ TextMessage, Message }

import com.typesafe.scalalogging.slf4j.StrictLogging
import io.gatling.core.check._
import io.gatling.core.check.extractor.xpath._
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.core.validation.{ Validation, FailureWrapper, SuccessWrapper }
import io.gatling.jms.JmsCheck
import net.sf.saxon.s9api.XdmNode
import org.w3c.dom.Document

object JmsXPathCheckBuilder extends StrictLogging {

  def preparer[T](f: StringReader => T)(message: Message): Validation[Option[T]] =
    try {
      message match {
        case tm: TextMessage => Some(f(new StringReader(tm.getText))).success
        case _               => "Unsupported message type".failure
      }
    } catch {
      case e: Exception =>
        val message = s"Could not parse response into a DOM Document: ${e.getMessage}"
        logger.info(message, e)
        message.failure
    }

  val SaxonXPathPreparer: Preparer[Message, Option[XdmNode]] = preparer(SaxonXPathExtractor.parse)

  val JDKXPathPreparer: Preparer[Message, Option[Document]] = preparer(JDKXPathExtractor.parse)

  val CheckBuilder: Extender[JmsCheck, Message] = (wrapped: Check[Message]) => wrapped

  def xpath(expression: Expression[String], namespaces: List[(String, String)]) =
    if (SaxonXPathExtractor.Enabled)
      new DefaultMultipleFindCheckBuilder[JmsCheck, Message, Option[XdmNode], String](CheckBuilder, SaxonXPathPreparer) {
        def findExtractor(occurrence: Int) = expression.map(new SaxonXPathExtractor.SingleXPathExtractor(_, namespaces, occurrence))
        def findAllExtractor = expression.map(new SaxonXPathExtractor.MultipleXPathExtractor(_, namespaces))
        def countExtractor = expression.map(new SaxonXPathExtractor.CountXPathExtractor(_, namespaces))
      }
    else
      new DefaultMultipleFindCheckBuilder[JmsCheck, Message, Option[Document], String](CheckBuilder, JDKXPathPreparer) {
        def findExtractor(occurrence: Int) = expression.map(new JDKXPathExtractor.SingleXPathExtractor(_, namespaces, occurrence))
        def findAllExtractor = expression.map(new JDKXPathExtractor.MultipleXPathExtractor(_, namespaces))
        def countExtractor = expression.map(new JDKXPathExtractor.CountXPathExtractor(_, namespaces))
      }
}
