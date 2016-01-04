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
package io.gatling.jdbc.feeder

import io.gatling.BaseSpec
import io.gatling.jdbc.JdbcSpec

class JdbcFeederSourceSpec extends BaseSpec with JdbcSpec {

  "JdbcFeederSource" should "be able to fetch data into a feeder" in withDatabase("customers", "feeder.sql") { url =>
    val feeder = JdbcFeederSource(url, Username, Password, "SELECT * FROM CUSTOMERS")
    feeder should have size 2
    val firstEntry = feeder(0)
    firstEntry("USER_ID") shouldBe 1
    firstEntry("FIRST_NAME") shouldBe "Foo"
    firstEntry("LAST_NAME") shouldBe "Bar"
    val secondEntry = feeder(1)
    secondEntry("USER_ID") shouldBe 2
    secondEntry("FIRST_NAME") shouldBe "Toto"
    secondEntry("LAST_NAME") shouldBe "Titi"
  }
}
