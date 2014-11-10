package io.gatling.http.integration

import org.scalatest.{ FlatSpec, Matchers }

import spray.http.HttpHeaders.{ Location, `Set-Cookie` }
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import spray.http._

import io.gatling.core.Predef._
import io.gatling.core.config.Protocols
import io.gatling.http.Predef._

class HttpIntegrationSpec extends FlatSpec with Matchers {

  "Gatling" should "send cookies returned in redirects in subsequent requests" in MockServerSupport { implicit testKit =>
    import MockServerSupport._
    import Checks._

    serverMock({
      case HttpRequest(GET, Uri.Path("/page1"), _, _, _) =>
        HttpResponse(status = 301, headers = List(Location("/page2"), `Set-Cookie`(HttpCookie("TestCookie1", "Test1"))))

      case HttpRequest(GET, Uri.Path("/page2"), _, _, _) =>
        HttpResponse(entity = "Hello World", headers = List(`Set-Cookie`(HttpCookie("TestCookie2", "Test2"))))

      case HttpRequest(GET, Uri.Path("/page3"), _, _, _) =>
        HttpResponse(entity = "Hello Again")
    })

    val session = runScenario(
      scenario("Cookie Redirect")
        .exec(
          http("/page1")
            .get("/page1")
            .check(
              // ScalaTest only defines a 'regex' matcher, we need to 'select it from Predef
              io.gatling.http.Predef.regex("Hello World"),
              currentLocation.is(s"http://localhost:$mockHttpPort/page2")))
        .exec(
          http("/page3")
            .get("/page3")
            .check(
              // ScalaTest only defines a 'regex' matcher, we need to 'select it from Predef
              io.gatling.http.Predef.regex("Hello Again"))))

    session.isFailed shouldBe false

    verifyRequestTo("/page1")
    verifyRequestTo("/page2", 1, checkCookie("TestCookie1", "Test1"))
    verifyRequestTo("/page3", 1, checkCookie("TestCookie1", "Test1"), checkCookie("TestCookie2", "Test2"))
  }

  it should "retrieve linked resources, when resource downloading is enabled" in MockServerSupport { implicit testKit =>
    import MockServerSupport._

    serverMock({
      case HttpRequest(GET, Uri.Path("/resourceTest/index.html"), _, _, _) =>
        HttpResponse(entity = file("resourceTest/index.html", `text/html`))

      case HttpRequest(GET, Uri.Path("/resourceTest/stylesheet.css"), _, _, _) =>
        HttpResponse(entity = file("resourceTest/stylesheet.css"))

      case HttpRequest(GET, Uri.Path("/resourceTest/img.png"), _, _, _) =>
        HttpResponse(entity = file("resourceTest/img.png"))

      case HttpRequest(GET, Uri.Path("/resourceTest/script.js"), _, _, _) =>
        HttpResponse(entity = file("resourceTest/script.js"))
    })

    val session = runScenario(
      scenario("Resource downloads")
        .exec(
          http("/resourceTest/index.html")
            .get("/resourceTest/index.html")
            .check(
              css("h1").is("Resource Test"),
              // ScalaTest only defines a 'regex' matcher, we need to 'select it from Predef
              io.gatling.http.Predef.regex("<title>Resource Test</title>"))),
      protocols = Protocols(MockServerSupport.httpProtocol.inferHtmlResources(BlackList(".*/bad_resource.png"))))

    session.isFailed shouldBe false

    verifyRequestTo("/resourceTest/index.html")
    verifyRequestTo("/resourceTest/stylesheet.css")
    verifyRequestTo("/resourceTest/script.js")
    verifyRequestTo("/resourceTest/img.png")
    verifyRequestTo("/bad_resource.png", 0)
  }

  it should "fetch resources in conditional comments" in MockServerSupport { implicit testKit =>
    import MockServerSupport._

    serverMock({
      case HttpRequest(GET, Uri.Path("/resourceTest/indexIE.html"), _, _, _) =>
        HttpResponse(entity = file("resourceTest/indexIE.html", `text/html`))

      case HttpRequest(GET, Uri.Path("/resourceTest/stylesheet.css"), _, _, _) =>
        HttpResponse(entity = file("resourceTest/stylesheet.css"))
    })

    val session = runScenario(
      scenario("Resource downloads")
        .exec(
          http("/resourceTest/indexIE.html")
            .get("/resourceTest/indexIE.html")
            .header("User-Agent",
              "Mozilla/5.0 (Windows; U; MSIE 9.0; WIndows NT 9.0; en-US)")),
      protocols = Protocols(MockServerSupport.httpProtocol.inferHtmlResources()))

    session.isFailed shouldBe false

    verifyRequestTo("/resourceTest/indexIE.html")
    verifyRequestTo("/resourceTest/stylesheet.css")

  }
}
