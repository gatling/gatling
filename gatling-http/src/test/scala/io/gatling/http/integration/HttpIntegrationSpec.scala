package io.gatling.http.integration

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import org.specs2.mutable.Specification
import WireMockSupport.wireMockPort
import io.gatling.core.config.Protocols

class HttpIntegrationSpec extends Specification {
  sequential

  "Gatling" should {
    "send cookies returned in redirects in subsequent requests" in WireMockSupport { implicit testKit =>
      stubFor(get(urlEqualTo("/page1"))
        .willReturn(
          aResponse()
            .withStatus(301)
            .withHeader("Location", "/page2")
            .withHeader("Set-Cookie", "TestCookie1=Test1")))

      stubFor(get(urlEqualTo("/page2"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Set-Cookie", "TestCookie2=Test2")
            .withBody("Hello World")))

      stubFor(get(urlEqualTo("/page3"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody("Hello Again")))

      val session = WireMockSupport.runScenario(
        scenario("Cookie Redirect")
          .exec(
            http("/page1")
              .get("/page1")
              .check(
                regex("Hello World"),
                currentLocation.is(s"http://localhost:$wireMockPort/page2")))
          .exec(
            http("/page3")
              .get("/page3")
              .check(regex("Hello Again"))))

      session.isFailed should_== (false)

      verify(getRequestedFor(urlEqualTo("/page1")))
      verify(getRequestedFor(urlEqualTo("/page2"))
        .withHeader("Cookie", WireMock.containing("TestCookie1=Test1")))
      verify(getRequestedFor(urlEqualTo("/page3"))
        .withHeader("Cookie", WireMock.containing("TestCookie1=Test1"))
        .withHeader("Cookie", WireMock.containing("TestCookie2=Test2")))
      success
    }

    "retrieve linked resources, when resource downloading is enabled" in WireMockSupport { implicit testKit =>
      stubFor(get(urlEqualTo("/resourceTest/index.html"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "text/html; charset=utf-8")
            .withBodyFile("/resourceTest/index.html")))

      stubFor(get(urlEqualTo("/resourceTest/stylesheet.css"))
        .willReturn(
          aResponse()
            .withBodyFile("/resourceTest/stylesheet.css")))

      stubFor(get(urlEqualTo("/resourceTest/img.png"))
        .willReturn(
          aResponse()
            .withBodyFile("/resourceTest/img.png")))

      stubFor(get(urlEqualTo("/resourceTest/script.js"))
        .willReturn(
          aResponse()
            .withBodyFile("/resourceTest/script.js")))

      val session = WireMockSupport.runScenario(
        scenario("Resource downloads")
          .exec(
            http("/resourceTest/index.html")
              .get("/resourceTest/index.html")
              .check(
                css("h1").is("Resource Test"),
                regex("<title>Resource Test</title>"))),
        protocols = Protocols(WireMockSupport.httpProtocol.fetchHtmlResources(BlackList(".*/bad_resource.png"))))

      session.isFailed should_== false
      verify(getRequestedFor(urlEqualTo("/resourceTest/index.html")))
      verify(getRequestedFor(urlEqualTo("/resourceTest/stylesheet.css")))
      verify(getRequestedFor(urlEqualTo("/resourceTest/script.js")))
      verify(getRequestedFor(urlEqualTo("/resourceTest/img.png")))
      verify(0, getRequestedFor(urlEqualTo("/bad_resource.png")))
      success
    }
  }
}
