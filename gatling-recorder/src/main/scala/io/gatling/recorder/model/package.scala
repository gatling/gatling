/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.recorder

package object model {

  case class HttpRequest(
      httpVersion: String,
      method:      String,
      uri:         String,
      headers:     Map[String, String],
      body:        Array[Byte],
      timestamp:   Long
  )

  case class HttpResponse(
      status:     Int,
      statusText: String,
      headers:    Map[String, String],
      body:       Array[Byte],
      timestamp:  Long
  )
}
