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

import io.gatling.core.check.DefaultMultipleFindCheckBuilder
import io.gatling.core.check.extractor.regex.{ CountRegexExtractor, GroupExtractor, MultipleRegexExtractor, SingleRegexExtractor }
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.http.check.{ HttpCheck, HttpCheckBuilders }
import io.gatling.http.response.Response

trait HttpBodyRegexOfType { self: HttpBodyRegexCheckBuilder[String] =>

  def ofType[X](implicit groupExtractor: GroupExtractor[X]) = new HttpBodyRegexCheckBuilder[X](expression)
}

object HttpBodyRegexCheckBuilder {

  def regex(expression: Expression[String]) = new HttpBodyRegexCheckBuilder[String](expression) with HttpBodyRegexOfType
}

class HttpBodyRegexCheckBuilder[X](private[body] val expression: Expression[String])(implicit groupExtractor: GroupExtractor[X])
    extends DefaultMultipleFindCheckBuilder[HttpCheck, Response, CharSequence, X](
      HttpCheckBuilders.stringBodyCheckFactory,
      HttpCheckBuilders.responseBodyStringPreparer) {

  def findExtractor(occurrence: Int) = expression.map(new SingleRegexExtractor(_, occurrence))
  def findAllExtractor = expression.map(new MultipleRegexExtractor(_))
  def countExtractor = expression.map(new CountRegexExtractor(_))
}
