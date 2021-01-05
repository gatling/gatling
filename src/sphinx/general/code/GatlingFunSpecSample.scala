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

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.funspec.GatlingHttpFunSpec

import GatlingFunSpecExample._

//#example-test
class GatlingFunSpecExample extends GatlingHttpFunSpec { // (1)

  val baseUrl = "http://example.com" // (2)
  override def httpProtocol = super.httpProtocol.header("MyHeader", "MyValue") // (3)

  spec { // (4)
    http("Example index.html test") // (5)
      .get("/index.html") // (6)
      .check(pageHeader.exists) // (7)
  }

}

object GatlingFunSpecExample {
  def pageHeader = css("h1") // (8)
}
//#example-test
