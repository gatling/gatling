/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
import io.gatling.core.Predef._
import io.gatling.core.session.Expression
import io.gatling.http.Predef._

class HttpRequest {

  {
    //#example-embedded-or-not
    // embedded style
    scenario("MyScenario")
      .exec(http("RequestName").get("url"))

    // non embedded style
    val request = http("RequestName").get("url")

    scenario("MyScenario")
      .exec(request)
    //#example-embedded-or-not
  }

  {
    val structure =
      """
    //#general-structure
    // general structure of an HTTP request
    http(requestName).method(url)
    //#general-structure
      """

    //#builtins-or-custom

    // concrete examples
    http("Retrieve home page").get("https://github.com/gatling/gatling")
    http("Login").post("https://github.com/session")
    http("Nginx cache purge").httpRequest("PURGE", "http://myNginx.com")
    //#builtins-or-custom
  }

  {
    //#getting-issues
    http("Getting issues")
      .get("https://github.com/gatling/gatling/issues?milestone=1&state=open")
    //#getting-issues

    //#query-params-no-el
    http("Getting issues")
      .get("https://github.com/gatling/gatling/issues")
      .queryParam("milestone", "1")
      .queryParam("state", "open")
    //#query-params-no-el

    //#query-params-with-el
    http("Value from session example")
      .get("https://github.com/gatling/gatling")
      .queryParam("myKey", "${sessionKey}")
    //#query-params-with-el

    //#query-param-no-value
    // GET https://github.com/gatling/gatling?myKey
    http("Empty value example")
      .get("https://github.com/gatling/gatling")
      .queryParam("myKey", "")
    //#query-param-no-value

    //#multivaluedQueryParam
    http("Request with multivaluedQueryParam")
      .get("myUrl")
      .multivaluedQueryParam("multi1", "${foo}") // where foo is the name of a Seq Session attribute
      .multivaluedQueryParam("multi2", session => Seq("foo", "bar"))
    //#multivaluedQueryParam

    //#queryParamSeq
    http("Getting issues")
      .get("https://github.com/gatling/gatling/issues")
      .queryParamSeq(Seq(("milestone", "1"), ("state", "open")))
    //#queryParamSeq

    //#queryParamMap
    http("Getting issues")
      .get("https://github.com/gatling/gatling/issues")
      .queryParamMap(Map("milestone" -> "1", "state" -> "open"))
    //#queryParamMap

    //#headers
    // Defining a map of headers before the scenario allows you to reuse these in several requests
    val sentHeaders = Map("Content-Type" -> "application/javascript", "Accept" -> "text/html")

    http("Custom headers")
      .post("myUrl")
      // Adds several headers at once
      .headers(sentHeaders)
      // Adds another header to the request
      .header("Keep-Alive", "150")
      // Overrides the Content-Type header
      .header("Content-Type", "application/json")
    //#headers

    //#asJSON
    http("foo").get("bar")
      .header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationJson)
      .header(HttpHeaderNames.Accept, HttpHeaderValues.ApplicationJson)
    //#asJSON

    //#asXML
    http("foo").get("bar")
      .header(HttpHeaderNames.ContentType, HttpHeaderValues.ApplicationXml)
      .header(HttpHeaderNames.Accept, HttpHeaderValues.ApplicationXml)
    //#asXML

    //#authentication
    http("My BASIC secured request").get("http://my.secured.uri").basicAuth("myUser", "myPassword")

    http("My DIGEST secured request").get("http://my.secured.uri").digestAuth("myUser", "myPassword")
    //#authentication

    //#outgoing-proxy
    http("Getting issues")
      .get("https://github.com/gatling/gatling/issues")
      .proxy(Proxy("myProxyHost", 8080).httpsPort(8143).credentials("myUsername","myPassword"))
    //#outgoing-proxy

    //#virtual-host
    // GET https://mobile.github.com/gatling/gatling instead of GET https://www.github.com/gatling/gatling
    http("Getting issues")
      .get("https://www.github.com/gatling/gatling/issues")
      .virtualHost("mobile")
    //#virtual-host

    val myCheck = status.is(200)
    //#check
    http("Getting issues")
      .get("https://www.github.com/gatling/gatling/issues")
      .check(myCheck)
    //#check

    //#ignoreDefaultChecks
    http("Getting issues")
      .get("https://www.github.com/gatling/gatling/issues")
      .ignoreDefaultChecks
    //#ignoreDefaultChecks

    //#disableFollowRedirect
    http("Getting issues")
      .get("https://www.github.com/gatling/gatling/issues")
      .disableFollowRedirect
    //#disableFollowRedirect

    //#silent
    http("Getting issues")
      .get("https://www.github.com/gatling/gatling/issues")
      .silent
    //#silent

    http("Getting issues")
      .get("https://www.github.com/gatling/gatling/issues")
      //#notSilent
      .resources(
        http("Gatling Logo")
          .get("http://gatling.io/assets/images/img1.png")
          .notSilent
      )
      //#notSilent

    //#formParam
    http("My Form Data")
      .post("my.form-action.uri")
      .formParam("myKey", "myValue")
    //#formParam

    //#formParamSeq
    http("My Form Data")
      .post("my.form-action.uri")
      .formParamSeq(Seq(("myKey", "myValue"), ("anotherKey", "anotherValue")))
    //#formParamSeq

    //#formParamMap
    http("My Form Data")
      .post("my.form-action.uri")
      .formParamMap(Map("myKey" -> "myValue", "anotherKey" -> "anotherValue"))
    //#formParamMap

    //#multivaluedFormParam
    http("Request with multivaluedFormParam")
      .post("myUrl")
      .multivaluedFormParam("multi1", "${foo}") // where foo is the name of a Seq Session attribute
      .multivaluedFormParam("multi2", session => List("foo", "bar"))
    //#multivaluedFormParam

    //#form
    http("Request with form")
      .post("myUrl")
      .form("${theForm}")
      .formParam("fieldToOverride", "newValue")
    //#form

    //#formUpload
    http("My Multipart Request")
      .post("my.form-action.uri")
      .formParam("myKey", "myValue")
      .formUpload("myKey2", "myAttachment.txt")
    //#formUpload

    val someGenerator: Expression[String] = "foo"
    http("foo")
      .get("bar")
      //#RawFileBody
      // myFileBody.json is a file that contains
      // { "myContent": "myHardCodedValue" }
      .body(RawFileBody("myFileBody.json")).asJSON
      //#RawFileBody
      //#ElFileBody
      // myFileBody.json is a file that contains
      // { "myContent": "${myDynamicValue}" }
      .body(ElFileBody("myFileBody.json")).asJSON
      //#ElFileBody
      //#StringBody
      .body(StringBody("""{ "myContent": "myHardCodedValue" }""")).asJSON

      .body(StringBody("""{ "myContent": "${myDynamicValue}" }""")).asJSON

      .body(StringBody(session => """{ "myContent": """" + someGenerator(session) + """" }""")).asJSON
      //#StringBody

    //#templates
    object Templates {
      val template: Expression[String] = (session: Session) =>
        for {
          foo <- session("foo").validate[String]
          bar <- session("bar").validate[String]
        } yield s"""{ foo: $foo, bar: $bar }"""
    }
    //#templates
  }

  {
    //#resp-processors-imports
    import org.asynchttpclient.util.Base64
    import io.gatling.http.response._
    import java.nio.charset.StandardCharsets.UTF_8
    //#resp-processors-imports

    http("foo").get("bar")
    //#response-processors

    // ignore when response isn't received (e.g. when connection refused)
    .transformResponse { case response if response.isReceived =>
      new ResponseWrapper(response) {
        override val body = new ByteArrayResponseBody(Base64.decode(response.body.string), UTF_8)
      }
    }
    //#response-processors
  }

  {
    //#resources
    http("Getting issues")
      .get("https://www.github.com/gatling/gatling/issues")
      .resources(
        http("api.js").get("https://collector-cdn.github.com/assets/api.js"),
        http("ga.js").get("https://ssl.google-analytics.com/ga.js")
      )
    //#resources
  }
}
