/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.http.internal

import java.util.function.BiConsumer

import io.gatling.commons.validation._
import io.gatling.core.session.{ Session => ScalaSession }
import io.gatling.http.client.Request
import io.gatling.javaapi.core.Session

object SignatureCalculators {
  def toScala(calculator: BiConsumer[Request, Session]): (Request, ScalaSession) => Validation[_] =
    (request, session) => calculator.accept(request, new Session(session)).success
}
