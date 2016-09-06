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
package io.gatling.http.action.async.sse

import io.gatling.core.json.Json

case class ServerSentEvent(
    name:  Option[String] = None,
    data:  Option[String] = None,
    id:    Option[String] = None,
    retry: Option[Int]    = None
) {

  def asJsonString: String = {

    // BEWARE: assume Map4 is implemented as an Array, so order is kept
    val map = Map("event" -> name, "id" -> id, "data" -> data, "retry" -> retry)
      .collect({ case (key, Some(value)) => (key, value) })

    Json.stringify(map, isRootObject = true)
  }
}
