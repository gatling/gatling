/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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
import io.gatling.core.check.Check.PreparedCache
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.response.Response

final case class HttpCheck(wrapped: Check[Response], scope: HttpCheckScope) extends Check[Response] {
  override def check(response: Response, session: Session, preparedCache: PreparedCache): Validation[CheckResult] =
    wrapped.check(response, session, preparedCache)

  override def checkIf(condition: (Response, Session) => Validation[Boolean]): HttpCheck = copy(wrapped.checkIf(condition))

  override def checkIf(condition: Expression[Boolean]): HttpCheck = copy(wrapped.checkIf(condition))
}

final class HttpCheckMaterializer[T, P](scope: HttpCheckScope, override val preparer: Preparer[Response, P])
    extends CheckMaterializer[T, HttpCheck, Response, P](HttpCheck(_, scope))
