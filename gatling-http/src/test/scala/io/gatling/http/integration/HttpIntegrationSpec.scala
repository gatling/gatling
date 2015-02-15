/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.integration

import io.gatling.core.CoreModule
import io.gatling.http.HttpModule
import io.gatling.http.ahc.{ HttpEngine, AhcHttpEngine }
import io.gatling.http.cache.HttpCaches
import io.gatling.http.config.DefaultHttpProtocol
import io.gatling.http.fetch.ResourceFetcher
import org.scalatest.FlatSpec
import org.scalatest.Matchers.{ regex => _, _ }
import org.scalatest.mock.MockitoSugar

import spray.http.HttpHeaders.{ Location, `Set-Cookie` }
import spray.http.HttpMethods._
import spray.http.MediaTypes._
import spray.http._

import io.gatling.core.config.{ GatlingConfiguration, Protocols }

class HttpIntegrationSpec extends FlatSpec with MockitoSugar with CoreModule with HttpModule {

  implicit val configuration = GatlingConfiguration.loadForTest()

  "Gatling" should "send cookies returned in redirects in subsequent requests" in {

    implicit val httpEngine: HttpEngine = new AhcHttpEngine
    implicit val httpCaches = new HttpCaches
    implicit val resourceFetcher = new ResourceFetcher
    implicit val defaultHttpProtocol = new DefaultHttpProtocol

    new MockServerSupport().exec { mockServerSupport =>
      implicit testKit =>

        import mockServerSupport._
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
                .check(
                  regex("Hello Again"))))

        session.isFailed shouldBe false

        verifyRequestTo("/page1")
        verifyRequestTo("/page2", 1, checkCookie("TestCookie1", "Test1"))
        verifyRequestTo("/page3", 1, checkCookie("TestCookie1", "Test1"), checkCookie("TestCookie2", "Test2"))
    }
  }

  it should "retrieve linked resources, when resource downloading is enabled" in {

    implicit val httpEngine: HttpEngine = new AhcHttpEngine
    implicit val httpCaches = new HttpCaches
    implicit val resourceFetcher = new ResourceFetcher
    implicit val defaultHttpProtocol = new DefaultHttpProtocol

    new MockServerSupport().exec { mockServerSupport =>
      implicit testKit =>
        import mockServerSupport._

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

        val session = mockServerSupport.runScenario(
          scenario("Resource downloads")
            .exec(
              http("/resourceTest/index.html")
                .get("/resourceTest/index.html")
                .check(
                  css("h1").is("Resource Test"),
                  regex("<title>Resource Test</title>"))),
          protocols = Protocols(mockServerSupport.httpProtocol.inferHtmlResources(BlackList(".*/bad_resource.png"))))

        session.isFailed shouldBe false

        verifyRequestTo("/resourceTest/index.html")
        verifyRequestTo("/resourceTest/stylesheet.css")
        verifyRequestTo("/resourceTest/script.js")
        verifyRequestTo("/resourceTest/img.png")
        verifyRequestTo("/bad_resource.png", 0)
    }
  }

  it should "fetch resources in conditional comments" in {

    implicit val httpEngine: HttpEngine = new AhcHttpEngine
    implicit val httpCaches = new HttpCaches
    implicit val resourceFetcher = new ResourceFetcher
    implicit val defaultHttpProtocol = new DefaultHttpProtocol

    new MockServerSupport().exec { mockServerSupport =>
      implicit testKit =>
        import mockServerSupport._

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
          protocols = Protocols(mockServerSupport.httpProtocol.inferHtmlResources()))

        session.isFailed shouldBe false

        verifyRequestTo("/resourceTest/indexIE.html")
        verifyRequestTo("/resourceTest/stylesheet.css")
    }
  }
}
