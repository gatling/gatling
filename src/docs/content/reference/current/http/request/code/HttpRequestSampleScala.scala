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

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.core.session.Expression
import io.gatling.http.Predef._

import java.io.ByteArrayInputStream
import java.util.Base64

class HttpRequestSampleScala {

  {
//#requestName
// with a static vale
http("requestName").get("https://gatling.io")
// with a static vale
http("${requestName}").get("https://gatling.io")
// with a static vale
http(session => session("requestName").as[String]).get("https://gatling.io")
//#requestName


//#inline
// inline style
scenario("ScenarioName")
  .exec(http("RequestName").get("url"))

// non inline style
val request = http("RequestName").get("url")

scenario("ScenarioName")
  .exec(request)
//#inline
  }

  {
//#methods
// with an absolute static url
http("name").get("https://gatling.io")
// with an absolute static url
http("name").get("${url}")
// with an absolute static url
http("name").get(session => session("url").as[String])

http("name").put("https://gatling.io")
http("name").post("https://gatling.io")
http("name").delete("https://gatling.io")
http("name").head("https://gatling.io")
http("name").patch("https://gatling.io")
http("name").options("https://gatling.io")
http("name").httpRequest("PURGE", "http://myNginx.com")
//#methods

//#full-query-in-url
http("Issues")
  .get("https://github.com/gatling/gatling/issues?milestone=1&state=open")
//#full-query-in-url

//#queryParam
// with static values
http("Issues")
  .get("https://github.com/gatling/gatling/issues")
  .queryParam("milestone", "1")
  .queryParam("state", "open")

// with Gatling EL strings
http("Issues")
  .get("https://github.com/gatling/gatling/issues")
  .queryParam("milestone", "${milestoneValue}")
  .queryParam("state", "${stateValue}")

// with functions
http("Issues")
  .get("https://github.com/gatling/gatling/issues")
  .queryParam("milestone", session => session("milestoneValue").as[String])
  .queryParam("state", session => session("stateValue").as[String])
//#queryParam

//#multivaluedQueryParam
http("name").get("/")
  // with static values
  .multivaluedQueryParam("param", Seq("value1", "value2"))

http("name").get("/")
  // with a Gatling EL string pointing to a Seq
  .multivaluedQueryParam("param", "${values}")

http("name").get("/")
  // with a function
  .multivaluedQueryParam("param", session => Seq("value1", "value2"))
//#multivaluedQueryParam

//#queryParam-multiple
http("name").get("/")
  .queryParamSeq(Seq(("key1", "value1"), ("key2", "value2")))

http("name").get("/")
  .queryParamMap(Map("key1" -> "value1", "key2" -> "value2"))
//#queryParam-multiple

//#headers
// Extracting a map of headers allows you to reuse these in several requests
val sentHeaders = Map(
  "Content-Type" -> "application/javascript",
  "Accept" -> "text/html"
)

http("name").get("/")
  // Adds several headers at once
  .headers(sentHeaders)
  // Adds another header to the request
  .header("Keep-Alive", "150")
  // Overrides the Content-Type header
  .header("Content-Type", "application/json")
//#headers

//#asXXX
// asJson
http("name").post("/")
  .asJson
// is a shortcut for:
http("name").post("/")
  .header("Accept", "application/json")
  .header("Content-Type", "application/json")

// asXml
http("name").post("/")
  .asXml
// is a shortcut for:
http("name").post("/")
  .header("Accept", "application/xhtml+xml")
  .header("Content-Type", "application/xhtml+xml")

// asFormUrlEncoded
http("name").post("/")
  .asFormUrlEncoded
// is a shortcut for:
http("name").post("/")
  .header("Content-Type", "application/application/x-www-form-urlencoded")

// asMultipartForm
http("name").post("/")
  .asMultipartForm
// is a shortcut for:
http("name").post("/")
  .header("Content-Type", "multipart/form-data")
//#asXXX

//#ignoreProtocolHeaders
http("name").get("/")
  .ignoreProtocolHeaders
//#ignoreProtocolHeaders

//#check
http("name").get("/")
  .check(status.is(200))
//#check

//#ignoreProtocolChecks
http("name").get("/")
  .ignoreProtocolChecks
//#ignoreProtocolChecks

//#StringBody
// with a static payload
http("name").post("/")
  .body(StringBody("""{ "foo": "staticValue" }"""))

// with a Gatling EL string payload
http("name").post("/")
  .body(StringBody("""{ "foo": "${dynamicValue}" }"""))

// with a function payload
http("name").post("/")
  .body(StringBody(session => s"""{ "foo": "${session("dynamicValueKey").as[String]}" }"""))
//#StringBody

//#template
object Templates {
  val template: Expression[String] = session =>
    for {
      foo <- session("foo").validate[String]
      bar <- session("bar").validate[String]
    } yield s"""{ "foo": "$foo", "bar": "$bar" }"""
}
//#template

//#template-usage
http("name").post("/")
  .body(StringBody(Templates.template))
//#template-usage

//#RawFileBody
// with a static path
http("name").post("/")
  .body(RawFileBody("rawPayload.json"))

// with a Gatling EL String path
http("name").post("/")
  .body(RawFileBody("${payloadPath}"))

// with a function path
http("name").post("/")
  .body(RawFileBody(session => session("payloadPath").as[String]))
//#RawFileBody

//#ElFileBody
http("name").post("/")
  .body(ElFileBody("rawPayload.json"))

// with a Gatling EL String path
http("name").post("/")
  .body(ElFileBody("${payloadPath}"))

// with a function path
http("name").post("/")
  .body(ElFileBody(session => session("payloadPath").as[String]))
//#ElFileBody

//#PebbleStringBody
http("name").post("/")
  .body(PebbleStringBody("""{ "foo": "{% if someCondition %}{{someValue}}{% endif %}" }"""))
//#PebbleStringBody

//#PebbleFileBody
// with a static value path
http("name").post("/")
  .body(PebbleFileBody("pebbleTemplate.json"))

// with a Gatling EL string path
http("name").post("/")
  .body(PebbleFileBody("${templatePath}"))

// with a function path
http("name").post("/")
  .body(PebbleFileBody(session => session("templatePath").as[String]))
//#PebbleFileBody

//#ByteArrayBody
// with a static value
http("name").post("/")
  .body(ByteArrayBody(Array[Byte](0, 1, 5, 4)))

// with a static value
http("name").post("/")
  .body(ByteArrayBody("${bytes}"))

// with a function
http("name").post("/")
  .body(ByteArrayBody(session =>
    Base64.getDecoder.decode(session("data").as[String]))
  )
//#ByteArrayBody

//#InputStreamBody
http("name").post("/")
  .body(InputStreamBody(session => new ByteArrayInputStream(Array[Byte](0, 1, 5, 4))))
//#InputStreamBody

//#formParam
// with static values
http("name").post("/")
  .formParam("milestone", "1")
  .formParam("state", "open")

// with Gatling EL strings
http("name").post("/")
  .formParam("milestone", "${milestoneValue}")
  .formParam("state", "${stateValue}")

// with functions
http("name").post("/")
  .formParam("milestone", session => session("milestoneValue").as[String])
  .formParam("state", session => session("stateValue").as[String])
//#formParam

//#multivaluedFormParam
http("name").post("/")
  // with static values
  .multivaluedFormParam("param", Seq("value1", "value2"))

http("name").post("/")
  // with a Gatling EL string pointing to a Seq
  .multivaluedFormParam("param", "${values}")

http("name").post("/")
  // with a function
  .multivaluedFormParam("param", session => Seq("value1", "value2"))
//#multivaluedFormParam

//#formParam-multiple
http("name").post("/")
  .formParamSeq(Seq(("key1", "value1"), ("key2", "value2")))

http("name").post("/")
  .formParamMap(Map("key1" -> "value1", "key2" -> "value2"))
//#formParam-multiple

//#formFull
http("name").post("/")
  .form("${previouslyCapturedForm}")
  // override an input
  .formParam("fieldToOverride", "newValue")
//#formFull

//#formUpload
// with a static filepath value
http("name").post("/")
  .formParam("key", "value")
  .formUpload("file1", "file1.dat")
  // you can set multiple files
  .formUpload("file2", "file2.dat")

// with a Gatling EL string filepath
http("name").post("/")
  .formUpload("file1", "${file1Path}")

// with a function filepath
http("name").post("/")
  .formUpload("file1", session => session("file1Path").as[String])
//#formUpload

//#bodyPart
// set a single part
http("name").post("/")
  .bodyPart(
    StringBodyPart("partName", "value")
  )

// set a multiple parts
http("name").post("/")
  .bodyParts(
    StringBodyPart("partName1", "value"),
    StringBodyPart("partName2", "value")
  )
//#bodyPart

//#bodyPart-options
http("name").post("/")
  .bodyPart(
    StringBodyPart("partName", "value")
      .contentType("contentType")
      .charset("utf-8")
      // part of the Content-Disposition header
      .fileName("fileName")
      // defaults to "form-data"
      .dispositionType("dispositionType")
      .contentId("contentId")
      .transferEncoding("transferEncoding")
      .header("name", "value")
  )
//#bodyPart-options

//#processRequestBody
http("name").post("/")
  .body(RawFileBody("file"))
  .processRequestBody(gzipBody)
//#processRequestBody

//#resources
http("name").get("/")
  .resources(
    http("api.js").get("/assets/api.js"),
    http("ga.js").get("/ga.js")
  )
//#resources

//#requestTimeout
http("name").get("/")
  .requestTimeout(3.minutes)
//#requestTimeout

//#silent
http("name").get("/")
  .silent
//#silent

http("name").get("/")
  //#notSilent
  .resources(
    http("resource")
      .get("/assets/images/img1.png")
      .notSilent
  )
//#notSilent

//#resp-processors-imports
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

import io.gatling.http.response._
//#resp-processors-imports

http("name").get("/")
//#response-processors
// ignore when response status code is not 200
.transformResponse {
  (response, session) =>
    if (response.status.code == 200) {
      response.copy(body = new ByteArrayResponseBody(Base64.getDecoder.decode(response.body.string), UTF_8))
    } else {
      response
    }
}
//#response-processors
  }
}
