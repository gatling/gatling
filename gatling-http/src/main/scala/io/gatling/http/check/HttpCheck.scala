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

package io.gatling.http.check

import io.gatling.commons.validation.Validation
import io.gatling.core.check.{ Check, CheckMaterializer, CheckResult, Preparer }
import io.gatling.core.session.Session
import io.gatling.http.response.Response

/**
 * This class serves as model for the HTTP-specific checks
 *
 * @param wrapped the underlying check
 * @param scope the part of the response this check targets
 */
final case class HttpCheck(wrapped: Check[Response], scope: HttpCheckScope) extends Check[Response] {
  override def check(response: Response, session: Session, preparedCache: Check.PreparedCache): Validation[CheckResult] =
    wrapped.check(response, session, preparedCache)
}

class HttpCheckMaterializer[T, P](scope: HttpCheckScope, override val preparer: Preparer[Response, P])
    extends CheckMaterializer[T, HttpCheck, Response, P](HttpCheck(_, scope))
