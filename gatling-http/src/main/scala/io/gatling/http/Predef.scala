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

package io.gatling.http

import io.gatling.core.check.Check
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckScope
import io.gatling.core.check.ConditionalCheck.ConditionalCheckWrapper
import io.gatling.core.check.ConditionalCheck
import io.gatling.core.check.ConditionalCheck.ConditionalCheck
import io.gatling.http.check.HttpCheck

object Predef extends HttpDsl {

  type Request = org.asynchttpclient.Request
  type Response = io.gatling.http.response.Response

  implicit object HttpConditionalCheckWrapper extends ConditionalCheckWrapper[Response, HttpCheck] {
    def wrap(check: ConditionalCheck[Response, HttpCheck]) = {
      val elseScope = check.elseCheck match { case Some(c) => c.scope case None => Set.empty }
      val elseResponseBodyUsageStrategy = check.elseCheck match { case Some(c) => c.responseBodyUsageStrategy case None => Set.empty }

      HttpCheck(check, check.thenCheck.scope ++ elseScope, check.thenCheck.responseBodyUsageStrategy ++ elseResponseBodyUsageStrategy)
    }
  }

}
