/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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
import io.gatling.core.Predef._
import io.gatling.core.session._
import io.gatling.http.Predef._

class FunctionSampleScala {
//#function
// inline usage with an anonymous function
exec(http("name")
  .get(session => s"/foo/${session("param").as[String].toLowerCase(Locale.getDefault)}"))

// passing a reference to a function
val f: Expression[String] =
  session => s"/foo/${session("param").as[String].toLowerCase(Locale.getDefault)}"
exec(http("name").get(f));
//#function
}
