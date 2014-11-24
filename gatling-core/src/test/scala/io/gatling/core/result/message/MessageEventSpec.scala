package io.gatling.core.result.message

import org.scalacheck.Gen.alphaStr
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{ FlatSpec, Matchers }

class MessageEventSpec extends FlatSpec with Matchers with GeneratorDrivenPropertyChecks {

  "MessageEvent.apply" should "return Start when passing 'START'" in {
    MessageEvent("START") shouldBe Start
  }

  it should "return End when passing 'END'" in {
    MessageEvent("END") shouldBe End
  }

  it should "throw an IllegalArgumentException on any other string" in {
    forAll(alphaStr.suchThat(s => s != "START" && s != "END")) { string =>
      an[IllegalArgumentException] should be thrownBy MessageEvent(string)
    }
  }
}
