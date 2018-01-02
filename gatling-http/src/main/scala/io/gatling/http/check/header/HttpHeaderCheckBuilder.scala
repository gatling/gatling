/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response

trait HttpHeaderCheckType

class HttpHeaderCheckBuilder(headerName: Expression[String]) extends DefaultMultipleFindCheckBuilder[HttpHeaderCheckType, Response, String] {
  override def findExtractor(occurrence: Int): Expression[SingleHttpHeaderExtractor] = headerName.map(new SingleHttpHeaderExtractor(_, occurrence))
  override def findAllExtractor: Expression[MultipleHttpHeaderExtractor] = headerName.map(new MultipleHttpHeaderExtractor(_))
  override def countExtractor: Expression[CountHttpHeaderExtractor] = headerName.map(new CountHttpHeaderExtractor(_))
}

object HttpHeaderProvider extends CheckProtocolProvider[HttpHeaderCheckType, HttpCheck, Response, Response] {

  override val specializer: Specializer[HttpCheck, Response] = HeaderSpecializer

  override val preparer: Preparer[Response, Response] = PassThroughResponsePreparer
}
