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

package io.gatling.http.check.url

import io.gatling.commons.validation.{ SuccessWrapper, Validation }
import io.gatling.core.check.{ CheckProtocolProvider, DefaultFindCheckBuilder, Preparer, Specializer }
import io.gatling.core.check.extractor._
import io.gatling.core.session._
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response

trait CurrentLocationCheckType

object CurrentLocationCheckBuilder {

  val CurrentLocation: DefaultFindCheckBuilder[CurrentLocationCheckType, Response, String] = {
    val extractor = new Extractor[Response, String] with SingleArity {
      val name = "currentLocation"
      def apply(prepared: Response): Validation[Some[String]] = Some(prepared.request.getUrl).success
    }.expressionSuccess

    new DefaultFindCheckBuilder[CurrentLocationCheckType, Response, String](extractor)
  }
}

object CurrentLocationProvider extends CheckProtocolProvider[CurrentLocationCheckType, HttpCheck, Response, Response] {

  override val specializer: Specializer[HttpCheck, Response] = UrlSpecializer

  override val preparer: Preparer[Response, Response] = PassThroughResponsePreparer
}
