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

import com.typesafe.scalalogging.StrictLogging

import io.gatling.core.check.{ DefaultMultipleFindCheckBuilder, Preparer }
import io.gatling.core.check.extractor.css._
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.core.util.StringHelper.RichString
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper }
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response
import jodd.lagarto.dom.NodeSelector

object HttpBodyCssCheckBuilder extends StrictLogging {

  def cssPreparer(implicit extractorFactory: CssExtractorFactory): Preparer[Response, NodeSelector] = (response: Response) =>
    try {
      extractorFactory.selectors.parse(response.body.string.unsafeChars).success

    } catch {
      case e: Exception =>
        val message = s"Could not parse response into a Jodd NodeSelector: ${e.getMessage}"
        logger.info(message, e)
        message.failure
    }

  def css(expression: Expression[String], nodeAttribute: Option[String])(implicit extractorFactory: CssExtractorFactory) =
    new DefaultMultipleFindCheckBuilder[HttpCheck, Response, NodeSelector, String](StringBodyExtender, cssPreparer) {

      import extractorFactory._

      def findExtractor(occurrence: Int) = expression.map(criterion => newSingleExtractor((criterion, nodeAttribute), occurrence))
      def findAllExtractor = expression.map(newMultipleExtractor(_, nodeAttribute))
      def countExtractor = expression.map(newCountExtractor(_, nodeAttribute))
    }
}
