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

import java.util.Locale
import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*

class FunctionSampleKotlin {
  init {
//#function
// inline usage with a Java lamdba
exec(http("name")
  .get { session -> "/foo/${session.getString("param")!!.toLowerCase(Locale.getDefault())}" })

// passing a reference to a function
val f =
  { session: Session -> "/foo/${session.getString("param")!!.toLowerCase(Locale.getDefault())}" }
exec(http("name").get(f))
//#function
  }
}
