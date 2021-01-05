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

package io.gatling.http.check.status

import io.gatling.commons.validation._
import io.gatling.core.check._
import io.gatling.core.session._
import io.gatling.http.check.{ HttpCheck, HttpCheckMaterializer }
import io.gatling.http.check.HttpCheckScope.Status
import io.gatling.http.response.Response

trait HttpStatusCheckType

object HttpStatusCheckBuilder
    extends DefaultFindCheckBuilder[HttpStatusCheckType, Response, Int](
      extractor = new FindExtractor[Response, Int]("status", response => Some(response.status.code).success).expressionSuccess,
      displayActualValue = true
    )

object HttpStatusCheckMaterializer {

  val Instance: CheckMaterializer[HttpStatusCheckType, HttpCheck, Response, Response] =
    new HttpCheckMaterializer[HttpStatusCheckType, Response](Status, identityPreparer)
}
