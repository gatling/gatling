/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.http.check.header

import io.gatling.core.check._
import io.gatling.core.session.{ Expression, RichExpression }
import io.gatling.http.check.{ HttpCheck, HttpCheckMaterializer }
import io.gatling.http.check.HttpCheckScope.Header
import io.gatling.http.response.Response

trait HttpHeaderCheckType

class HttpHeaderCheckBuilder(headerName: Expression[CharSequence])
    extends DefaultMultipleFindCheckBuilder[HttpHeaderCheckType, Response, String](displayActualValue = true) {
  override protected def findExtractor(occurrence: Int): Expression[Extractor[Response, String]] = headerName.map(HttpHeaderExtractors.find(_, occurrence))
  override protected def findAllExtractor: Expression[Extractor[Response, Seq[String]]] = headerName.map(HttpHeaderExtractors.findAll)
  override protected def countExtractor: Expression[Extractor[Response, Int]] = headerName.map(HttpHeaderExtractors.count)
}

object HttpHeaderCheckMaterializer {

  val Instance: CheckMaterializer[HttpHeaderCheckType, HttpCheck, Response, Response] =
    new HttpCheckMaterializer[HttpHeaderCheckType, Response](Header, identityPreparer)
}
