package io.gatling.http.integration

import io.gatling.core.Predef._
import io.gatling.core.config.Protocols
import io.gatling.http.Predef._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import spray.http.HttpHeaders.{ Location, `Set-Cookie` }
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import spray.http._

@RunWith(classOf[JUnitRunner])
class HttpIntegrationSpec extends Specification {
  sequential

  "Gatling" should {
    "send cookies returned in redirects in subsequent requests" in MockServerSupport { implicit testKit =>
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
                regex("Hello World"),
                currentLocation.is(s"http://localhost:$mockHttpPort/page2")))
          .exec(
            http("/page3")
              .get("/page3")
              .check(regex("Hello Again"))))

      session.isFailed should beFalse

      verifyRequestTo("/page1")
      verifyRequestTo("/page2", 1, hasCookie("TestCookie1", "Test1"))
      verifyRequestTo("/page3", 1, hasCookie("TestCookie1", "Test1"), hasCookie("TestCookie2", "Test2"))
      success
    }

    "retrieve linked resources, when resource downloading is enabled" in MockServerSupport { implicit testKit =>
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
                regex("<title>Resource Test</title>"))),
        protocols = Protocols(MockServerSupport.httpProtocol.inferHtmlResources(BlackList(".*/bad_resource.png"))))

      session.isFailed should beFalse

      verifyRequestTo("/resourceTest/index.html")
      verifyRequestTo("/resourceTest/stylesheet.css")
      verifyRequestTo("/resourceTest/script.js")
      verifyRequestTo("/resourceTest/img.png")
      verifyRequestTo("/bad_resource.png", 0)

      success
    }

    "fetch resources in conditional comments" in MockServerSupport { implicit testKit =>
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
                "Mozilla/5.0 (Windows; U; MSIE 9.0; WIndows NT 9.0; en-US))")),
        protocols = Protocols(MockServerSupport.httpProtocol.inferHtmlResources()))

      session.isFailed should beFalse

      verifyRequestTo("/resourceTest/indexIE.html")
      verifyRequestTo("/resourceTest/stylesheet.css")

      success
    }
  }
}
