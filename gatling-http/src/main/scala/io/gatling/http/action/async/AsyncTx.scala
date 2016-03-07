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
package io.gatling.http.action.async

import io.gatling.core.check.CheckResult
import io.gatling.core.session.Session
import io.gatling.http.check.async.AsyncCheck
import io.gatling.http.protocol.HttpProtocol
import io.gatling.core.action.Action

import org.asynchttpclient.Request

case class AsyncTx(
    session:               Session,
    next:                  Action,
    requestName:           String,
    request:               Request,
    protocol:              HttpProtocol,
    start:                 Long,
    reconnectCount:        Int                      = 0,
    check:                 Option[AsyncCheck]       = None,
    pendingCheckSuccesses: List[CheckResult]        = Nil,
    updates:               List[Session => Session] = Nil
) {

  def applyUpdates(session: Session) = {
    val newSession = session.update(updates)
    copy(session = newSession, updates = Nil)
  }
}
