package io.gatling.http.check.body

import java.nio.charset.StandardCharsets._

import io.gatling.core.Predef._
import io.gatling.core.check.CheckResult
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.core.test.ValidationValues
import io.gatling.http.Predef._
import io.gatling.http.response.{Response, StringResponseBody}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable
import scala.xml.Elem

class HttpBodyXPathCheckSpec extends FlatSpec with Matchers with ValidationValues with MockitoSugar {

  GatlingConfiguration.setUpForTest()

  implicit def cache = mutable.Map.empty[Any, Any]

  val session = Session("mockSession", "mockUserName")

  def mockResponse(xml: Elem): Response = {
    val response = mock[Response]
    when(response.body) thenReturn StringResponseBody(xml.toString(), UTF_8)
    when(response.hasResponseBody) thenReturn true
    response
  }

  "xpath.find.exists" should "find single result" in {

    val response = mockResponse(<id>1072920417</id>)

    xpath("/id", Nil).find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some("1072920417"), None)
  }

  it should "find first occurrence" in {

    val response = mockResponse(<root>
      <id>1072920417</id> <id>1072920418</id>
    </root>)

    xpath("//id").find.exists.build.check(response, session).succeeded shouldBe CheckResult(Some("1072920417"), None)
  }

  "xpath.findAll.exists" should "find all occurrences" in {

    val response = mockResponse(<root>
      <id>1072920417</id> <id>1072920418</id>
    </root>)

    xpath("//id").findAll.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(Seq("1072920417", "1072920418")), None)
  }

  it should "fail when finding nothing instead of returning an empty Seq" in {

    val response = mockResponse(<root>
      <id>1072920417</id> <id>1072920418</id>
    </root>)

    xpath("//fo").findAll.exists.build.check(response, session).failed shouldBe "xpath(//fo).findAll.exists, found nothing"
  }

  "xpath.count.exists" should "find all occurrences" in {

    val response = mockResponse(<root>
      <id>1072920417</id> <id>1072920418</id>
    </root>)

    xpath("//id").count.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(2), None)
  }

  it should "return 0 when finding nothing instead of failing" in {

    val response = mockResponse(<root>
      <id>1072920417</id> <id>1072920418</id>
    </root>)

    xpath("//fo").count.exists.build.check(response, session).succeeded shouldBe CheckResult(Some(0), None)
  }
}
