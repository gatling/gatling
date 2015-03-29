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