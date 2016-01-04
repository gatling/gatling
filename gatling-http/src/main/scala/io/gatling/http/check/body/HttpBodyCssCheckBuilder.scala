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
package io.gatling.http.check.body

import io.gatling.commons.util.StringHelper._
import io.gatling.commons.validation._
import io.gatling.core.check.{ DefaultMultipleFindCheckBuilder, Preparer }
import io.gatling.core.check.extractor.css._
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response
import jodd.lagarto.dom.NodeSelector

trait HttpBodyCssOfType {
  self: HttpBodyCssCheckBuilder[String] =>

  def ofType[X: NodeConverter](implicit extractorFactory: CssExtractorFactory) = new HttpBodyCssCheckBuilder[X](expression, nodeAttribute)
}

object HttpBodyCssCheckBuilder {

  private val ErrorMapper = "Could not parse response into a Jodd NodeSelector: " + _

  def cssPreparer(implicit extractorFactory: CssExtractorFactory): Preparer[Response, NodeSelector] = (response: Response) =>
    safely(ErrorMapper) {
      extractorFactory.selectors.parse(response.body.string.unsafeChars).success
    }

  def css(expression: Expression[String], nodeAttribute: Option[String])(implicit extractorFactory: CssExtractorFactory) =
    new HttpBodyCssCheckBuilder[String](expression, nodeAttribute) with HttpBodyCssOfType
}

class HttpBodyCssCheckBuilder[X: NodeConverter](private[body] val expression: Expression[String], private[body] val nodeAttribute: Option[String])(implicit extractorFactory: CssExtractorFactory)
    extends DefaultMultipleFindCheckBuilder[HttpCheck, Response, NodeSelector, X](StringBodyExtender, HttpBodyCssCheckBuilder.cssPreparer) {

  import extractorFactory._

  def findExtractor(occurrence: Int) = expression.map(criterion => newSingleExtractor((criterion, nodeAttribute), occurrence))
  def findAllExtractor = expression.map(newMultipleExtractor(_, nodeAttribute))
  def countExtractor = expression.map(newCountExtractor(_, nodeAttribute))
}
