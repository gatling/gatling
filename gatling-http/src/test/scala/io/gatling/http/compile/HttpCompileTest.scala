/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.http.compile

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import javax.net.ssl.KeyManagerFactory

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import io.netty.handler.codec.http.HttpMethod

class HttpCompileTest extends Simulation {

  registerPebbleExtensions(null: com.mitchellbosecke.pebble.extension.Extension)
  registerJmesPathFunctions(null: io.burt.jmespath.function.Function)

  private val httpProtocol = http
    .baseUrl("http://172.30.5.143:8080")
    .baseUrls("http://172.30.5.143:8080", "http://172.30.5.143:8081")
    .virtualHost("172.30.5.143:8080")
    .proxy(Proxy("172.31.76.106", 8080))
    .proxy(Proxy("172.31.76.106", 8080).credentials("username", "password"))
    .proxy(Proxy("172.31.76.106", 8080).httpsPort(8081))
    .proxy(Proxy("172.31.76.106", 8080).http)
    .proxy(Proxy("172.31.76.106", 8080).socks4)
    .proxy(Proxy("172.31.76.106", 8080).socks5)
    .noProxyFor("localhost")
    .acceptHeader("*/*")
    .acceptCharsetHeader("ISO-8859-1,utf-8;q=0.7,*;q=0.3")
    .acceptEncodingHeader("gzip,deflate,sdch")
    .acceptLanguageHeader("fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4")
    .authorizationHeader("Basic XXXXX")
    .connectionHeader("Close")
    .contentTypeHeader("aplication/json")
    .doNotTrackHeader("AAA")
    .userAgentHeader(
      "Mozilla/5.0 (X11; Linux i686) AppleWebKit/535.19 (KHTML, like Gecko) Ubuntu/12.04 Chromium/18.0.1025.151 Chrome/18.0.1025.151 Safari/535.19"
    )
    .header("foo", "bar")
    .header(io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE, "bar")
    .header(HttpHeaderNames.ContentType, "bar")
    .headers(Map("foo" -> "bar"))
    .headers(Map(io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE -> "bar"))
    .check(bodyString.transform(_.length).lt(100000))
    .check(bodyString.transformWithSession((string, session) => string.length).lte(100000))
    .check(bodyString.transformOption(stringO => stringO.map(_.length)).gt(100000))
    .check(bodyString.transformOptionWithSession((stringO, session) => stringO.map(_.length)).gte(100000))
    .check(bodyBytes.is("foo".getBytes(UTF_8)))
    .check(md5.is("XXXXX"))
    .check(sha1.is("XXXXX"))
    .check(responseTimeInMillis.is(100))
    .check(
      form("#form")
        .transform { foo: Map[String, Any] =>
          foo
        }
        .saveAs("theForm")
    )
    .disableFollowRedirect
    .maxRedirects(5)
    .disableAutoReferer
    .disableWarmUp
    .warmUp("https://gatling.io")
    .inferHtmlResources(white = WhiteList(".*\\.html"))
    .maxConnectionsPerHost(6)
    .shareConnections
    .perUserNameResolution
    .localAddress("192.168.1.100")
    .localAddresses(List("192.168.1.100", "192.168.1.101"))
    .useAllLocalAddresses
    .disableCaching
    .disableUrlEncoding
    .silentUri("https://foo\\.com/*")
    .silentResources
    .basicAuth("foo", "bar")
    .digestAuth("foo", "bar")
    .nameInferredHtmlResourcesAfterUrlTail
    .nameInferredHtmlResourcesAfterAbsoluteUrl
    .nameInferredHtmlResourcesAfterRelativeUrl
    .nameInferredHtmlResourcesAfterPath
    .nameInferredHtmlResourcesAfterLastPathElement
    .nameInferredHtmlResources(_.getPath)
    .asyncNameResolution()
    .asyncNameResolution("8.8.8.8", "8.8.4.4")
    .asyncNameResolution(Array(new InetSocketAddress("8.8.8.8", 53), new InetSocketAddress("8.8.4.4", 53)))
    .hostNameAliases(Map("foo" -> List("127.0.0.1")))
    .enableHttp2
    .http2PriorKnowledge(Map("www.google.com" -> true, "gatling.io" -> false))
    .perUserKeyManagerFactory(_ => KeyManagerFactory.getInstance("TLS"))

  private val testData3 = Array(Map("foo" -> "bar")).circular

  private val scn = scenario("Scn")
    // method
    .exec(http("Request").get("/"))
    .exec(http("Request").put("/"))
    .exec(http("Request").post("/"))
    .exec(http("Request").patch("/"))
    .exec(http("Request").head("/"))
    .exec(http("Request").delete("/"))
    .exec(http("Request").options("/"))
    .exec(http("Request").httpRequest(HttpMethod.valueOf("JSON"), "/support/get-plot-data?chartID=66"))
    // url function
    .exec(http("Request").get(_ => "/"))
    // headers
    .exec(http("Request").get(_ => "/").header("foo", "${bar}"))
    .exec(http("Request").get(_ => "/").header("foo", _ => "bar"))
    .exec(http("Request").get(_ => "/").headers(Map("foo" -> "${bar}")))
    .exec(http("Request").get(_ => "/").headers(Map(io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE -> "${bar}")))
    // queryParam
    .exec(http("Request").get("/").queryParam("param", "one"))
    .exec(http("Request").get("/").queryParam("param1", "one").queryParam("param2", "two"))
    .exec(http("Request").get("/").queryParam("param", "${param}"))
    .exec(http("Request").get("/").queryParam("param", _ => "foo"))
    .exec(http("Request").get("/").queryParam("foo", (_: Session) => io.gatling.commons.validation.Success(1)))
    // multivaluedQueryParam
    .exec(http("Request").get("/").multivaluedQueryParam("param", List("foo")))
    .exec(http("Request").get("/").multivaluedQueryParam("param", "${foo}"))
    .exec(http("Request").get("/").multivaluedQueryParam("param", List("foo")))
    // queryParamSeq
    .exec(http("Request").get("/").queryParamSeq(Seq("foo" -> "${bar}")))
    // queryParamMap
    .exec(http("Request").get("/").queryParamMap(Map("foo" -> "${bar}")))
    // auth
    .exec(http("Request").get("/").basicAuth("usr", "pwd"))
    .exec(http("Request").get("/").digestAuth("usr", "pwd"))
    // requestTimeout
    .exec(http("Request").get("/").requestTimeout(3.minutes))
    // misc
    .exec(
      http("Request").get("/").silent.notSilent.disableUrlEncoding.disableFollowRedirect.ignoreProtocolChecks.ignoreProtocolHeaders
    )
    // check
    .exec(
      http("Request")
        .get("/")
        .check(
          status.in(200 to 210).saveAs("blablaParam"),
          status.in(200, 210).saveAs("blablaParam"),
          status.in(Seq(200, 304)).saveAs("blablaParam"),
          header("HEADER").is("BAR"),
          headerRegex("location", ".*&id_token=(.*)&state=.*").find.exists,
          headerRegex("location", ".*&id_token=(.*)&state=.*").is("BAR"),
          currentLocation.is("https://gatling.io"),
          currentLocationRegex("code=(.+)&"),
          currentLocationRegex("foo").find.exists,
          bodyBytes.is(Array.fill(5)(1.toByte)),
          bodyBytes.is(RawFileBody("foobar.txt")),
          bodyLength.lte(100000),
          bodyStream.transform(is => "").saveAs("foo"),
          bodyString.is("foo"),
          bodyString.is(ElFileBody("foobar.txt")),
          css(".foo"),
          css("#foo", "href"),
          css(".foo").ofType[Node].count.is(1),
          css(".foo").notExists,
          css("#foo").ofType[Node].transform(_.getNodeName),
          css(".foo").findRandom.is("some text"),
          css(".foo").findRandom(5).is(Seq("some text")),
          jsonPath("//foo/bar[2]/baz"),
          jsonPath("$..foo").is("bar"),
          jsonPath("$..foo").ofType[String].is("bar"),
          jsonPath("$..foo").ofType[Int].is(1),
          jsonPath("$..foo").ofType[Seq[Any]].is(Seq("foo")),
          jsonPath("$..foo").ofType[Map[String, Any]].is(Map[String, Any]("foo" -> 1)),
          jsonPath("$..foo.bar[2].baz").transform(_ + "foo"),
          jsonPath("$..foo.bar[2].baz").transformWithSession((string, session) => string + "foo"),
          jsonPath("$..foo.bar[2].baz").transformOption(_.map(_ + "foo")),
          jsonPath("$..foo.bar[2].baz").transformOptionWithSession((maybeString, session) => maybeString.map(_ + "foo")),
          jsonpJsonPath("$..foo").is("bar"),
          jmesPath("[].friends[].name"),
          jmesPath("[].friends[].name").is("bar"),
          jmesPath("[].friends[].name").ofType[String].is("bar"),
          jmesPath("[].friends[].name").ofType[Int].is(1),
          jmesPath("[].friends[].name").ofType[Seq[Any]].is(Seq("foo")),
          jmesPath("[].friends[].name").ofType[Map[String, Any]].is(Map[String, Any]("foo" -> 1)),
          jsonpJmesPath("foo").is("bar"),
          regex("""<input id="text1" type="text" value="aaaa" />""").optional.saveAs("var1"),
          regex("""<input id="text1" type="text" value="aaaa" />""").count.is(1),
          regex("""<input id="text1" type="test" value="aaaa" />""").notExists,
          substring("foo").exists,
          xpath("//input[@id='text1']/@value"),
          xpath("//input[@id='text1']/@value").find,
          xpath("//input[@id='text1']/@value").find.exists,
          xpath("//input[@id='text1']/@value").find.is("expected"),
          xpath("//input[@id='text1']/@value").find.exists.saveAs("key"),
          xpath("//input[@id='text1']/@value").saveAs("key"),
          xpath("//input[@id='text1']/@value").findAll,
          xpath("//input[@id='text1']/@value").count,
          xpath("//input[@id='text1']/@value").name("This is a check"),
          xpath("//input[@value='${aaaa_value}']/@id").name("foo").saveAs("sessionParam"),
          xpath("//input[@value='aaaa']/@id").not("param"),
          xpath("//input[@id='${aaaa_value}']/@value").notExists,
          xpath("//input[@id='text1']/@value").is("aaaa").saveAs("test2"),
          md5.is("0xA59E79AB53EEF2883D72B8F8398C9AC3"),
          sha1.is("0xA59E79AB53EEF2883D72B8F8398C9AC3"),
          responseTimeInMillis.lt(1000)
        )
    )
    .exec(http("Request").get("/tests").check(header(io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE).is("text/html; charset=utf-8")))
    // form
    .exec(
      http("Request")
        .post("/")
        .form("${theForm}")
        .formParam("baz", "${qix}")
        .formParamSeq(Seq("foo" -> "${bar}"))
        .formParamMap(Map("foo" -> "${bar}"))
        .multivaluedFormParam("foo", Seq("bar"))
    )
    .exec(http("Request").post("/").multivaluedFormParam("foo", "${bar}"))
    // resources
    .exec(
      http("Request")
        .get("/")
        .resources(
          http("Request").post("/").multivaluedFormParam("foo", "${bar}"),
          http("Request").get("/").queryParam("param", "foo"),
          http("Request").get("/").queryParam("param", "${foo}"),
          http("Request").get("/").queryParam("param", session => "foo")
        )
    )
    // body
    .exec(http("Request").post("/things").body(StringBody("FOO${BAR}BAZ")).asJson)
    .exec(http("Request").post("/things").body(ElFileBody("create_thing.txt")))
    .exec(http("Request").post("/things").body(RawFileBody("create_thing.txt")))
    .exec(http("Request").post("/things").body(PebbleStringBody("create_thing.txt")))
    .exec(http("Request").post("/things").body(PebbleFileBody("create_thing.txt")))
    .exec(http("Request").post("/things").body(ByteArrayBody("${bytes}")))
    // processRequestBody
    .exec(http("Request").post("/things").body(StringBody("FOO${BAR}BAZ")).processRequestBody(identity))
    .exec(http("Request").post("/things").body(ByteArrayBody("${bytes}")).processRequestBody(gzipBody))
    .exec(http("Request").post("/things").body(ByteArrayBody("${bytes}")).processRequestBody(streamBody))
    // bodyParts
    .exec(
      http("Request")
        .post("url")
        .formUpload("name", "path")
        .bodyPart(RawFileBodyPart("name", "path"))
        .bodyPart(ElFileBodyPart("name", "path"))
        .bodyPart(ElFileBodyPart("name", "path").contentType("foo"))
        .bodyPart(PebbleFileBodyPart("name", "path"))
        .bodyPart(PebbleStringBodyPart("name", "somePebbleString"))
    )
    // sign
    .exec(
      http("Request")
        .get("/foo/bar?baz=qix")
        .signWithOAuth1("consumerKey", "clientSharedSecret", "token", "tokenSecret")
    )
    .exec(
      http("Request")
        .get("/foo/bar?baz=qix")
        .signWithOAuth1("consumerKey", "clientSharedSecret", "token", "tokenSecret")
    )
    .exec(
      http("Request")
        .get("/foo/bar?baz=qix")
        .sign(new SignatureCalculator {
          override def sign(request: Request): Unit = {
            import java.util.Base64
            import javax.crypto.Mac
            import javax.crypto.spec.SecretKeySpec
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(new SecretKeySpec("THE_SECRET_KEY".getBytes("UTF-8"), "HmacSHA256"))
            val rawSignature = mac.doFinal(request.getUri.getQuery.getBytes("UTF-8"))
            val authorization = Base64.getEncoder.encodeToString(rawSignature)
            request.getHeaders.add("Authorization", authorization)
          }
        })
    )
    // proxy
    .exec(http("Request").head("/").proxy(Proxy("172.31.76.106", 8080).httpsPort(8081)))
    .exec(http("Request").head("/").proxy(Proxy("172.31.76.106", 8080).socks4))
    .exec(http("Request").head("/").proxy(Proxy("172.31.76.106", 8080).socks5))
    // polling
    .exec(polling.every(10).exec(http("poll").get("/foo")))
    .exec(polling.pollerName("poll").every(10).exec(http("poll").get("/foo")))
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
    .exec(http("Request").get("/").transformResponse { (_, response) =>
      import io.gatling.http.response._
      response.copy(body = new StringResponseBody(response.body.string.replace(")]}',", ""), response.body.charset))
    })

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)

  // Conditional check compile test
  private val requestWithUntypedCheckIf =
    http("untypedCheckIf")
      .get("/")
      .check(
        checkIf("${bool}") {
          jsonPath("$..foo")
        }
      )

  def isJsonResponse(response: Response): Boolean =
    response
      .header(io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE)
      .exists(_.contains(io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON.toString))

  private val requestWithTypedCheckIf =
    http("typedCheckIf")
      .get("/")
      .check(
        checkIf((response: Response, _: Session) => isJsonResponse(response)) {
          jsonPath("$..foo")
        }
      )

  //[fl]
  //
  //[fl]
}
