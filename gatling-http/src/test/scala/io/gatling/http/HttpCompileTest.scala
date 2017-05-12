/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
package io.gatling.http

import scala.concurrent.duration._

import io.gatling.commons.validation.Success
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.check.HttpCheck

class HttpCompileTest extends Simulation {

  val httpProtocol = http
    .baseURL("http://172.30.5.143:8080")
    .proxy(Proxy("172.31.76.106", 8080).httpsPort(8081))
    .noProxyFor("localhost")
    .acceptHeader("*/*")
    .acceptCharsetHeader("ISO-8859-1,utf-8;q=0.7,*;q=0.3")
    .acceptLanguageHeader("fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4")
    .acceptEncodingHeader("gzip,deflate,sdch")
    .userAgentHeader("Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/12.04 Chromium/18.0.1025.151 Chrome/18.0.1025.151 Safari/535.19")
    .check(bodyString.transform(string => string.length).lessThan(100000))
    .check(bodyString.transform((string, session) => string.length).lessThan(100000))
    .check(bodyString.transformOption(stringO => stringO.map(_.length)).lessThan(100000))
    .check(bodyString.transformOption((stringO, session) => stringO.map(_.length)).lessThan(100000))
    .check(bodyBytes.is("foo".getBytes()))
    .check(md5.is("XXXXX"))
    .check(sha1.is("XXXXX"))
    .check(responseTimeInMillis.is(100))
    .check(form("#form").transform { foo: Map[String, Seq[String]] => foo }.saveAs("theForm"))
    .disableCaching
    .disableWarmUp
    .warmUp("http://gatling.io")
    .inferHtmlResources(white = WhiteList(".*\\.html"))
    .hostNameAliases(Map("foo" -> "127.0.0.1"))

  val httpConfToVerifyDumpSessionOnFailureBuiltIn = http.extraInfoExtractor(dumpSessionOnFailure)

  val httpConfToVerifyUserProvidedInfoExtractors = http
    .extraInfoExtractor(extraInfo => List(extraInfo.requestName, extraInfo.response.body.string))

  val testData3 = Array(Map("foo" -> "bar")).circular

  val scn = scenario("Scn")
    // method
    .exec(http("Request").get("/"))
    .exec(http("Request").put("/"))
    .exec(http("Request").post("/"))
    .exec(http("Request").patch("/"))
    .exec(http("Request").head("/"))
    .exec(http("Request").delete("/"))
    .exec(http("Request").options("/"))
    .exec(http("Request").httpRequest("JSON", "/support/get-plot-data?chartID=66"))

    // url function
    .exec(http("Request").get(session => "/"))
    // queryParam
    .exec(http("Request").get("/").queryParam("param", "one"))
    .exec(http("Request").get("/").queryParam("param1", "one").queryParam("param2", "two"))
    .exec(http("Request").get("/").queryParam("param", "${param}"))
    .exec(http("Request").get("/").queryParam("param", session => "foo"))
    .exec(http("Request").get("/").queryParam("foo", (session: Session) => io.gatling.commons.validation.Success(1)))
    // multivaluedQueryParam
    .exec(http("Request").get("/").multivaluedQueryParam("param", List("foo")))
    .exec(http("Request").get("/").multivaluedQueryParam("param", "${foo}"))
    .exec(http("Request").get("/").multivaluedQueryParam("param", List("foo")))
    // basic auth
    .exec(http("Request").get("/").basicAuth("usr", "pwd"))
    // check
    .exec(http("Request").get("/").check(xpath("//input[@id='text1']/@value")))
    .exec(http("Request").get("/").check(xpath("//input[@id='text1']/@value"), jsonPath("//foo/bar[2]/baz")))
    .exec(http("Request").get("/").check(xpath("//input[@id='text1']/@value").find))
    .exec(http("Request").get("/").check(xpath("//input[@id='text1']/@value").find.exists))
    .exec(http("Request").get("/").check(xpath("//input[@id='text1']/@value").find.is("expected")))
    .exec(http("Request").get("/").check(xpath("//input[@id='text1']/@value").find.exists.saveAs("key")))
    .exec(http("Request").get("/").check(xpath("//input[@id='text1']/@value").saveAs("key")))
    .exec(http("Request").get("/").check(xpath("//input[@id='text1']/@value").findAll))
    .exec(http("Request").get("/").check(xpath("//input[@id='text1']/@value").count))
    .exec(
      http("Request").get("h/")
        .check(
          bodyBytes.is(Array.fill(5)(1.toByte)),
          bodyBytes.is(RawFileBody("foobar.txt")),
          css(".foo"),
          css("#foo", "href"),
          css(".foo").ofType[Node].count.is(1),
          css(".foo").notExists,
          css("#foo").ofType[Node].transform { node: Node => node.getNodeName },
          css(".foo").findRandom.is("some text"),
          css(".foo").findRandom(5).is(Seq("some text")),
          jsonPath("$..foo").is("bar"),
          jsonpJsonPath("$..foo").is("bar"),
          regex("""<input id="text1" type="text" value="aaaa" />""").optional.saveAs("var1"),
          regex("""<input id="text1" type="text" value="aaaa" />""").count.is(1),
          regex("""<input id="text1" type="test" value="aaaa" />""").notExists,
          xpath("//input[@value='${aaaa_value}']/@id").saveAs("sessionParam"),
          bodyString.is("foo"),
          bodyString.is(ElFileBody("foobar.txt")),
          substring("foo").exists,
          xpath("//input[@id='${aaaa_value}']/@value").notExists,
          xpath("//input[@value='aaaa']/@id").not("param"),
          xpath("//input[@id='text1']/@value").is("aaaa").saveAs("test2"),
          md5.is("0xA59E79AB53EEF2883D72B8F8398C9AC3"),
          sha1.is("0xA59E79AB53EEF2883D72B8F8398C9AC3"),
          header("FOO").is("BAR"),
          headerRegex("FOO", "code=(.+)&").is("BAR"),
          status.in(200 to 210).saveAs("blablaParam"),
          status.in(200, 210).saveAs("blablaParam"),
          status.in(Seq(200, 304)).saveAs("blablaParam"),
          responseTimeInMillis.lessThan(1000),
          currentLocation.is("http://gatling.io"),
          currentLocationRegex("code=(.+)&")
        )
    )
    .exec(http("Request").get("/tests").check(header(HttpHeaderNames.ContentType).is("text/html; charset=utf-8")))
    // form
    .exec(http("Request").post("/")
      .form("${theForm}")
      .formParam("baz", "${qix}")
      .multivaluedFormParam("foo", Seq("bar")))
    .exec(http("Request").post("/").multivaluedFormParam("foo", "${bar}"))
    // resources
    .exec(http("Request").get("/")
      .resources(
        http("Request").post("/").multivaluedFormParam("foo", "${bar}"),
        http("Request").get("/").queryParam("param", "foo"),
        http("Request").get("/").queryParam("param", "${foo}"),
        http("Request").get("/").queryParam("param", session => "foo")
      ))
    // body
    .exec(http("Request").post("/things").body(StringBody("FOO${BAR}BAZ")).asJSON)
    .exec(http("Request").post("/things").body(ElFileBody("create_thing.txt")))
    .exec(http("Request").post("/things").body(RawFileBody("create_thing.txt")))
    .exec(http("Request").post("/things").body(PebbleStringBody("create_thing.txt")))
    .exec(http("Request").post("/things").body(PebbleFileBody("create_thing.txt")))
    // bodyParts
    .exec(http("Request").post("url")
      .formUpload("name", "path")
      .bodyPart(RawFileBodyPart("name", "path"))
      .bodyPart(ElFileBodyPart("name", "path")))
    // proxy
    .exec(http("Request").head("/").proxy(Proxy("172.31.76.106", 8080).httpsPort(8081)))
    // polling
    .exec(polling.every(10.seconds).exec(http("poll").get("/foo")))
    .exec(polling.pollerName("poll").every(10.seconds).exec(http("poll").get("/foo")))
    .exec(polling.pollerName("poll").stop)
    .exec(polling.stop)
    // rendezVous
    .rendezVous(100)
    // addCookie
    .exec(addCookie(Cookie("foo", "bar").withDomain("foo.com")))
    // getCookieValue
    .exec(getCookieValue(CookieKey("foo").withDomain("foo.com").saveAs("newName")))
    // flushSessionCookies
    .exec(flushSessionCookies)
    // flushCookieJar
    .exec(flushCookieJar)
    // flushHttpCache
    .exec(flushHttpCache)
    // transformResponse
    .exec(http("Request").get("/").transformResponse {
      case response if response.isReceived =>

        import io.gatling.http.response._

        new ResponseWrapper(response) {
          override val body = new StringResponseBody(response.body.string.replace(")]}',", ""), response.charset)
        }
    })

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
    .assertions(
      global.allRequests.count.is(0),
      global.allRequests.count.lt(10),
      global.allRequests.count.lte(10),
      global.allRequests.count.gt(10),
      global.allRequests.count.gte(10),
      global.allRequests.count.in(10, 20),
      global.successfulRequests.percent.gte(90.0),
      global.failedRequests.percent.lte(10.0),
      details("foo" / "bar").allRequests.count.is(0),
      forAll.responseTime.min.lt(10),
      forAll.responseTime.max.lt(1000),
      forAll.responseTime.stdDev.lt(10),
      forAll.responseTime.percentile(99.99).lt(1000),
      forAll.responseTime.percentile1.lt(1000),
      forAll.responseTime.percentile2.lt(1000),
      forAll.responseTime.percentile3.lt(1000),
      forAll.responseTime.percentile4.lt(1000)
    )

  // Conditional check compile test
  val requestWithUntypedCheckIf =
    http("untypedCheckIf").get("/")
      .check(
        checkIf("${bool}") {
          jsonPath("$..foo")
        }
      )

  def isJsonResponse(response: Response): Boolean = response.header(HttpHeaderNames.ContentType).exists(_.contains(HttpHeaderValues.ApplicationJson))

  val requestWithTypedCheckIf =
    http("typedCheckIf").get("/")
      .check(
        checkIf((response: Response, _: Session) => isJsonResponse(response)) {
          jsonPath("$..foo")
        }
      )

  //[fl]
  //
  //[fl]
}
