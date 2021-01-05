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

package io.gatling.http.engine.response

import io.gatling.commons.validation.Failure
import io.gatling.core.check.Check
import io.gatling.core.session.Session
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckScope._
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper

object CheckProcessor {

  private[response] def check(session: Session, response: Response, checks: List[HttpCheck]): (Session, Option[Failure]) = {
    val filteredChecks =
      if (HttpHelper.isNotModified(response.status)) {
        checks.filter(c => c.scope != Chunks && c.scope != Body)
      } else {
        checks
      }

    Check.check(response, session, filteredChecks)
  }
}
