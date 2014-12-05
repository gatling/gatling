/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.sse

import io.gatling.core.json.JSON

case class ServerSentEvent(
    data: Option[String] = None,
    name: Option[String] = None,
    id: Option[String] = None,
    retry: Option[Int] = None) {

  def asJSONString(): String = {
    val map =
      Map("event" -> name, "id" -> id, "data" -> data, "retry" -> retry)
        .collect({ case (key, Some(value)) => (key, value) })

    JSON.stringify(map, isRootObject = true)
  }
}
