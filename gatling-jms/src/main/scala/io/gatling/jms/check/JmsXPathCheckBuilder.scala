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

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.check._
import io.gatling.core.check.extractor.xpath.{ CountXPathExtractor, MultipleXPathExtractor, SingleXPathExtractor, XPathExtractor }
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper }
import net.sf.saxon.s9api.XdmNode
import javax.jms.{ TextMessage, Message }
import scala.Some
import io.gatling.jms.JmsCheck
import java.io.StringReader

object JmsXPathCheckBuilder extends StrictLogging {

  val preparer: Preparer[Message, Option[XdmNode]] = (response: Message) =>
    try {
      val root = response match {
        case tm: TextMessage => Some(XPathExtractor.parse(new StringReader(tm.getText)))
        case _               => None
      }
      root.success
    } catch {
      case e: Exception =>
        val message = s"Could not parse response into a DOM Document: ${e.getMessage}"
        logger.info(message, e)
        message.failure
    }

  val checkBuilder: CheckFactory[JmsCheck, Message] = (wrapped: Check[Message]) => wrapped

  def xpath(expression: Expression[String], namespaces: List[(String, String)]) =
    new JmsMultipleCheckBuilder[Option[XdmNode], String](checkBuilder, preparer) {
      def findExtractor(occurrence: Int) = expression.map(new SingleXPathExtractor(_, namespaces, occurrence))

      def findAllExtractor = expression.map(new MultipleXPathExtractor(_, namespaces))

      def countExtractor = expression.map(new CountXPathExtractor(_, namespaces))
    }
}
