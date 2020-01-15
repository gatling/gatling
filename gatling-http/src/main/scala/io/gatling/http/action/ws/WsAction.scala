/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action.ws

import io.gatling.commons.util.TypeCaster
import io.gatling.commons.validation._
import io.gatling.core.session.Session
import io.gatling.http.action.ws.fsm.WsFsm

object WsAction {
  implicit val WsFsmTypeCaster: TypeCaster[WsFsm] = new TypeCaster[WsFsm] {
    @throws[ClassCastException]
    override def cast(value: Any): WsFsm =
      value match {
        case v: WsFsm => v
        case _        => throw new ClassCastException(cceMessage(value, classOf[WsFsm]))
      }

    override def validate(value: Any): Validation[WsFsm] =
      value match {
        case v: WsFsm => v.success
        case _        => cceMessage(value, classOf[WsFsm]).failure
      }
  }
}

trait WsAction {

  // import optimized TypeCaster
  import WsAction._

  final def fetchFsm(actorName: String, session: Session): Validation[WsFsm] =
    session(actorName)
      .validate[WsFsm]
      .mapError(m => s"Couldn't fetch open webSocket: $m")
}
