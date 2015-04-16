/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.integration

import org.jboss.netty.handler.codec.http._

import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.HeaderNames._
import io.gatling.http.HttpSpec
import io.gatling.core.CoreModule
import io.gatling.http.HttpModule
import io.gatling.http.check.HttpCheckSupport
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.cache.HttpCaches
import io.gatling.http.config.DefaultHttpProtocol

class HttpIntegrationSpec extends HttpSpec with CoreModule with HttpModule {

  // FIXME
  object RegexSupport extends HttpCheckSupport
  val regexCheck = RegexSupport.regex _

  implicit val configuration = GatlingConfiguration.loadForTest()

  "Gatling" should "send cookies returned in redirects in subsequent requests" in {
    implicit val httpCaches = new HttpCaches
    implicit val httpEngine = new HttpEngine
    implicit val defaultHttpProtocol = new DefaultHttpProtocol

    val handler: Handler = {
      case HttpRequest(HttpMethod.GET, "/page1") =>
        val cookieEncoder = new CookieEncoder(true)
        cookieEncoder.addCookie("TestCookie1", "Test1")
        val headers = Map(SetCookie -> cookieEncoder.encode(), Location -> "/page2")
        sendResponse(status = HttpResponseStatus.MOVED_PERMANENTLY, headers = headers)

      case HttpRequest(HttpMethod.GET, "/page2") =>
        val cookieEncoder = new CookieEncoder(true)
        cookieEncoder.addCookie("TestCookie2", "Test2")
        val headers = Map(SetCookie -> cookieEncoder.encode())
        sendResponse(content = "Hello World", headers = headers)

      case HttpRequest(HttpMethod.GET, "/page3") =>
        sendResponse(content = "Hello Again")
    }

    runWithHttpServer(handler) { implicit httpServer =>

      val session = runScenario(
        scenario("Cookie Redirect")
          .exec(
            http("/page1")
              .get("/page1")
              .check(
                regexCheck("Hello World"),
                currentLocation.is(s"http://localhost:$mockHttpPort/page2")))
          .exec(
            http("/page3")
              .get("/page3")
              .check(
                regexCheck("Hello Again"))))

      session.isFailed shouldBe false

      verifyRequestTo("/page1")
      verifyRequestTo("/page2", 1, checkCookie("TestCookie1", "Test1"))
      verifyRequestTo("/page3", 1, checkCookie("TestCookie1", "Test1"), checkCookie("TestCookie2", "Test2"))
    }
  }

  it should "retrieve linked resources, when resource downloading is enabled" in {

    implicit val httpCaches = new HttpCaches
    implicit val httpEngine = new HttpEngine
    implicit val defaultHttpProtocol = new DefaultHttpProtocol

    val handler: Handler = {
      case HttpRequest(HttpMethod.GET, path) =>
        sendFile(path.drop(1)) // Drop leading slash in path
    }

    runWithHttpServer(handler) { implicit httpServer =>
      val session = runScenario(
        scenario("Resource downloads")
          .exec(
            http("/resourceTest/index.html")
              .get("/resourceTest/index.html")
              .check(
                css("h1").is("Resource Test"),
                regexCheck("<title>Resource Test</title>"))),
        protocolCustomizer = _.inferHtmlResources(BlackList(".*/bad_resource.png")))

      session.isFailed shouldBe false

      verifyRequestTo("/resourceTest/index.html")
      verifyRequestTo("/resourceTest/stylesheet.css")
      verifyRequestTo("/resourceTest/script.js")
      verifyRequestTo("/resourceTest/img.png")
      verifyRequestTo("/resourceTest/bad_resource.png", 0)
    }
  }

  it should "fetch resources in conditional comments" in {

    implicit val httpCaches = new HttpCaches
    implicit val httpEngine = new HttpEngine
    implicit val defaultHttpProtocol = new DefaultHttpProtocol

    val handler: Handler = {
      case HttpRequest(HttpMethod.GET, path) =>
        sendFile(path.drop(1)) // Drop leading slash in path
    }

    runWithHttpServer(handler) { implicit httpServer =>
      val session = runScenario(
        scenario("Resource downloads")
          .exec(
            http("/resourceTest/indexIE.html")
              .get("/resourceTest/indexIE.html")
              .header("User-Agent",
                "Mozilla/5.0 (Windows; U; MSIE 9.0; WIndows NT 9.0; en-US)")),
        protocolCustomizer = _.inferHtmlResources())

      session.isFailed shouldBe false

      verifyRequestTo("/resourceTest/indexIE.html")
      verifyRequestTo("/resourceTest/stylesheet.css")
    }
  }
}
