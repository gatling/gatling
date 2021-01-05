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

package io.gatling.http.action.sse.fsm

import io.gatling.core.json.Json
import io.gatling.netty.util.StringBuilderPool

final case class ServerSentEvent(
    name: Option[String],
    data: Option[String],
    id: Option[String],
    retry: Option[Int]
) {

  def asJsonString: String = {
    val sb = StringBuilderPool.DEFAULT.get().append('{')
    name.foreach { value =>
      sb.append("\"event\":\"").append(value).append("\",")
    }
    id.foreach { value =>
      sb.append("\"id\":\"").append(value).append("\",")
    }
    data.foreach { value =>
      sb.append("\"data\":\"").append(Json.stringify(value, true)).append("\",")
    }
    retry.foreach { value =>
      sb.append("\"retry\":").append(retry).append(",")
    }
    sb.setLength(sb.length - 1)
    sb.append('}').toString
  }
}
