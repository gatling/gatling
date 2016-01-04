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

import io.gatling.commons.validation._
import io.gatling.core.check.{ DefaultMultipleFindCheckBuilder, Preparer }
import io.gatling.core.check.extractor.jsonpath._
import io.gatling.core.json.JsonParsers
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response

trait HttpBodyJsonpJsonPathOfType {
  self: HttpBodyJsonpJsonPathCheckBuilder[String] =>

  def ofType[X: JsonFilter](implicit extractorFactory: JsonPathExtractorFactory) = new HttpBodyJsonpJsonPathCheckBuilder[X](path, jsonParsers)
}

object HttpBodyJsonpJsonPathCheckBuilder {

  val JsonpRegex = """^\w+(?:\[\"\w+\"\]|\.\w+)*\((.*)\);?\s*$""".r
  val JsonpRegexFailure = "Regex could not extract JSON object from JSONP response".failure

  def parseJsonpString(string: String, jsonParsers: JsonParsers): Validation[Any] = string match {
    case JsonpRegex(jsonp) => jsonParsers.safeParse(jsonp)
    case _                 => JsonpRegexFailure
  }

  def jsonpPreparer(jsonParsers: JsonParsers): Preparer[Response, Any] = response => parseJsonpString(response.body.string, jsonParsers)

  def jsonpJsonPath(path: Expression[String])(implicit extractorFactory: JsonPathExtractorFactory, jsonParsers: JsonParsers) =
    new HttpBodyJsonpJsonPathCheckBuilder[String](path, jsonParsers) with HttpBodyJsonpJsonPathOfType
}

class HttpBodyJsonpJsonPathCheckBuilder[X: JsonFilter](
  private[body] val path:        Expression[String],
  private[body] val jsonParsers: JsonParsers
)(implicit extractorFactory: JsonPathExtractorFactory)
    extends DefaultMultipleFindCheckBuilder[HttpCheck, Response, Any, X](
      StringBodyExtender,
      HttpBodyJsonpJsonPathCheckBuilder.jsonpPreparer(jsonParsers)
    ) {

  import extractorFactory._

  def findExtractor(occurrence: Int) = path.map(newSingleExtractor[X](_, occurrence))
  def findAllExtractor = path.map(newMultipleExtractor[X])
  def countExtractor = path.map(newCountExtractor)
}
