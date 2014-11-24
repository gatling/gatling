package io.gatling.core.result.message

import org.scalacheck.Gen.alphaStr
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{ FlatSpec, Matchers }

class StatusSpec extends FlatSpec with Matchers with GeneratorDrivenPropertyChecks {

  "Status.apply" should "return OK when passing 'OK'" in {
    Status("OK") shouldBe OK
  }

  it should "return OK when passing 'KO'" in {
    Status("KO") shouldBe KO
  }

  it should "throw an IllegalArgumentException on any other string" in {
    forAll(alphaStr.suchThat(s => s != "OK" && s != "KO")) { string =>
      an[IllegalArgumentException] should be thrownBy Status(string)
    }
  }
}
