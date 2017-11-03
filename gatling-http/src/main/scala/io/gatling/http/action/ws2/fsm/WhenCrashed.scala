/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
package io.gatling.http.action.ws2.fsm

trait WhenCrashed { this: WsActor =>

  when(Crashed) {
    case Event(ClientCloseRequest(actionName, session, next), CrashedData(errorMessage)) =>
      val newSession = errorMessage match {
        case Some(mess) =>
          val newSession = session.markAsFailed
          statsEngine.logCrash(newSession, actionName, s"Client issued close order but WebSocket was already crashed: $mess")
          newSession
        case _ =>
          logger.info("Client issued close order but WebSocket was already closed")
          session
      }

      next ! newSession.remove(wsName)
      stop()

    case Event(message: SendTextMessage, CrashedData(errorMessage)) =>
      // FIXME sent message so be stashed until reconnect, instead of failed
      val loggedMessage = errorMessage match {
        case Some(mess) => s"Client issued message but WebSocket was already crashed: $mess"
        case _          => "Client issued message but WebSocket was already closed"
      }

      logger.info(loggedMessage)

      // perform blocking reconnect
      gotoConnecting(message.session, Right(message))
  }
}
