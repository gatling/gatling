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

package io.gatling.http.check.status

import io.gatling.commons.validation._
import io.gatling.core.check._
import io.gatling.core.check.extractor._
import io.gatling.core.session._
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckBuilders._
import io.gatling.http.response.Response

trait HttpStatusCheckType

object HttpStatusCheckBuilder {

  val Status: DefaultFindCheckBuilder[HttpStatusCheckType, Response, Int] = {
    val statusExtractor = new Extractor[Response, Int] with SingleArity {
      val name = "status"
      def apply(prepared: Response): Validation[Option[Int]] = Some(prepared.status.code).success
    }.expressionSuccess

    new DefaultFindCheckBuilder[HttpStatusCheckType, Response, Int](statusExtractor, displayActualValue = true)
  }
}

object HttpStatusCheckMaterializer extends CheckMaterializer[HttpStatusCheckType, HttpCheck, Response, Response] {

  override val specializer: Specializer[HttpCheck, Response] = StatusSpecializer

  override val preparer: Preparer[Response, Response] = PassThroughResponsePreparer
}
