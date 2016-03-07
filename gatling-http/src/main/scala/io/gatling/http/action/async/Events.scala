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

import io.gatling.core.action.Action
import io.gatling.core.session.Session
import io.gatling.http.check.async.AsyncCheck

trait AsyncEvent
case class OnFailedOpen(tx: AsyncTx, errorMessage: String, time: Long) extends AsyncEvent
case class CheckTimeout(check: AsyncCheck) extends AsyncEvent

trait UserAction {
  def requestName: String
  def next: Action
  def session: Session
}

case class SetCheck(requestName: String, check: AsyncCheck, next: Action, session: Session) extends UserAction
case class CancelCheck(requestName: String, next: Action, session: Session) extends UserAction
case class Close(requestName: String, next: Action, session: Session) extends UserAction
case class Reconciliate(requestName: String, next: Action, session: Session) extends UserAction
