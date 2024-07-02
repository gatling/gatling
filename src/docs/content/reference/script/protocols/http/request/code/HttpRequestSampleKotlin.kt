/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import io.gatling.http.response.ByteArrayResponseBody
import io.gatling.http.response.Response
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.http.HttpDsl.*
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*
import java.util.function.Function

class HttpRequestSampleKotlin {

init {
//#requestName
// with a static value
http("requestName").get("https://gatling.io")
// with a dynamic value computed from a Gatling Expression Language String
http("#{requestName}").get("https://gatling.io")
// with a dynamic value computed from a function
http { session -> session.getString("requestName") }.get("https://gatling.io")
//#requestName

//#inline
// inline style
scenario("scenarioName")
  .exec(http("requestName").get("url"))

// non inline style
val request = http("RequestName").get("url")

scenario("MyScenario")
  .exec(request)
//#inline
}

init {
//#methods
// with an absolute static url
http("name").get("https://gatling.io")
// with an absolute static url
http("name").get("#{url}")
// with an absolute static url
http("name").get { session -> session.getString("url") }

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
http("Issues").get("https://github.com/gatling/gatling/issues")
  .queryParam("milestone", "1")
  .queryParam("state", "open")

// with Gatling EL strings
http("Issues").get("https://github.com/gatling/gatling/issues")
  .queryParam("milestone", "#{milestoneValue}")
  .queryParam("state", "#{stateValue}")

// with functions
http("Issues").get("https://github.com/gatling/gatling/issues")
  .queryParam("milestone") { session -> session.getString("milestoneValue") }
  .queryParam("state") { session -> session.getString("stateValue") }
//#queryParam


//#multivaluedQueryParam
http("name").get("/")
  // with static values
  .multivaluedQueryParam("param", listOf("value1", "value2"))

http("name").get("/")
  // with a Gatling EL string pointing to a List
  .multivaluedQueryParam("param", "#{values}")

http("name").get("/")
  // with a function
  .multivaluedQueryParam("param") { session -> listOf("value1", "value2") }
//#multivaluedQueryParam

//#queryParam-multiple
http("name").get("/")
  .queryParamSeq(listOf(
    AbstractMap.SimpleEntry<String, Any?>("key1", "value1"),
    AbstractMap.SimpleEntry<String, Any?>("key2", "value2")
  ))

val params = mapOf(
  "key1" to "value1",
  "key2" to "value2"
)

http("name").get("/")
  .queryParamMap(params)
//#queryParam-multiple

//#headers
// Extracting a map of headers allows you to reuse these in several requests
val sentHeaders = mapOf(
  "content-type" to "application/javascript",
  "accept" to "text/html"
)

http("name").get("/")
  // Add several headers at once
  .headers(sentHeaders)
  // Adds another header to the request
  .header("keep-alive", "150")
  // Overrides the content-type header
  .header("content-type", "application/json")
//#headers

//#asXXX
// asJson
http("name").post("/")
  .asJson()
// is a shortcut for:
http("name").post("/")
  .header("accept", "application/json")
  // only for requests that have a body
  .header("content-type", "application/json")

// asXml
http("name").post("/")
  .asXml()
// is a shortcut for:
http("name").post("/")
  .header("accept", "application/xhtml+xml")
  // only for requests that have a body
  .header("content-type", "application/xhtml+xml")

// asFormUrlEncoded
http("name").post("/")
  .asFormUrlEncoded()
// is a shortcut for:
http("name").post("/")
  // only for requests that have a body
  .header("content-type", "application/application/x-www-form-urlencoded")

// asMultipartForm
http("name").post("/")
  .asMultipartForm()
// is a shortcut for:
http("name").post("/")
  // only for requests that have a body
  .header("content-type", "multipart/form-data")
//#asXXX

//#ignoreProtocolHeaders
http("name").get("/")
  .ignoreProtocolHeaders()
//#ignoreProtocolHeaders

//#check
http("name").get("/")
  .check(status().shouldBe(200))
//#check

//#ignoreProtocolChecks
http("name").get("/")
  .ignoreProtocolChecks()
//#ignoreProtocolChecks

//#StringBody
// with a static payload
http("name").post("/")
  .body(StringBody("""{ "foo": "staticValue" }"""))

// with a Gatling EL string payload
http("name").post("/")
  .body(StringBody("""{ "foo": "#{dynamicValue}" }"""))

// with a function payload
http("name").post("/")
  .body(StringBody { session -> """{ "foo": "${session.getString("dynamicValueKey")}" }""" })
//#StringBody
}

//#template
internal object Templates {
  val template = Function { session: Session ->
    val foo = session.getString("foo")
    val bar = session.getString("bar")
    """{ "foo": "$foo", "bar": "$bar" }"""
  }
}
//#template

init {
//#template-usage
  http("name").post("/")
    .body(StringBody(HttpRequestSampleJava.Templates.template))
//#template-usage

//#RawFileBody
// with a static path
http("name").post("/")
  .body(RawFileBody("rawPayload.json"))

// with a Gatling EL String path
http("name").post("/")
  .body(RawFileBody("#{payloadPath}"))

// with a function path
http("name").post("/")
  .body(RawFileBody { session -> session.getString("payloadPath") })
//#RawFileBody

//#ElFileBody
http("name").post("/")
  .body(ElFileBody("rawPayload.json"))

// with a Gatling EL String path
http("name").post("/")
  .body(ElFileBody("#{payloadPath}"))

// with a function path
http("name").post("/")
  .body(ElFileBody { session -> session.getString("payloadPath") })
//#ElFileBody

//#PebbleStringBody
http("name").post("/")
  .body(PebbleStringBody("{ \"foo\": \"{% if someCondition %}{{someValue}}{% endif %}\" }"))
//#PebbleStringBody

//#PebbleFileBody
// with a static value path
http("name").post("/")
  .body(PebbleFileBody("pebbleTemplate.json"))

// with a Gatling EL string path
http("name").post("/")
  .body(PebbleFileBody("#{templatePath}"))

// with a function path
http("name").post("/")
  .body(PebbleFileBody { session -> session.getString("templatePath") })
//#PebbleFileBody

//#ByteArrayBody
// with a static value
http("name").post("/")
  .body(ByteArrayBody(byteArrayOf(0, 1, 5, 4)))

// with a static value
http("name").post("/")
  .body(ByteArrayBody("#{bytes}"))

// with a function
http("name").post("/")
  .body(ByteArrayBody { session -> Base64.getDecoder().decode(session.getString("data")) }
  )
//#ByteArrayBody

//#InputStreamBody
http("name").post("/")
  .body(InputStreamBody { session -> ByteArrayInputStream(byteArrayOf(0, 1, 5, 4)) })
//#InputStreamBody

//#formParam
// with static values
http("name").post("/")
  .formParam("milestone", "1")
  .formParam("state", "open")

// with Gatling EL strings
http("name").post("/")
  .formParam("milestone", "#{milestoneValue}")
  .formParam("state", "#{stateValue}")

// with functions
http("name").post("/")
  .formParam("milestone") { session -> session.getString("milestoneValue") }
  .formParam("state") { session -> session.getString("stateValue") }
//#formParam

//#multivaluedFormParam
http("name").post("/") // with static values
  .multivaluedFormParam("param", Arrays.asList<Any>("value1", "value2"))

http("name").post("/") // with a Gatling EL string pointing to a List
  .multivaluedFormParam("param", "#{values}")

http("name").post("/") // with a function
  .multivaluedFormParam("param") { session -> listOf("value1", "value2") }
//#multivaluedFormParam

//#formParam-multiple
  http("name").post("/")
    .formParamSeq(listOf(
      AbstractMap.SimpleEntry<String, String>("key1", "value1"),
      AbstractMap.SimpleEntry<String, String>("key2", "value2")
    ))

  http("name").post("/")
    .formParamMap(mapOf("key1" to "value1", "key2" to "value2"))
//#formParam-multiple

//#formFull
http("name").post("/")
  .form("#{previouslyCapturedForm}") // override an input
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
  .formUpload("file1", "#{file1Path}")

// with a function filepath
http("name").post("/")
  .formUpload("file1") { session -> session.getString("file1Path") }
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
http("name").post("/")
  .resources(
    http("api.js")["/assets/api.js"],
    http("ga.js")["/ga.js"]
  )
//#resources

//#requestTimeout
http("name").get("/")
  .requestTimeout(Duration.ofMinutes(3))
//#requestTimeout

//#silent
http("name").get("/")
  .silent()
//#silent

//#silent
http("name").get("/") //#notSilent
  .resources(
    http("resource").get("/assets/images/img1.png")
      .notSilent()
  )
//#notSilent

/*
//#resp-processors-imports
import io.gatling.http.response.*;

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64
//#resp-processors-imports
*/

http("name").post("/")
//#response-processors
// ignore when response status code is not 200
.transformResponse { response, session ->
  if (response.status().code() == 200) {
    return@transformResponse Response(
      response.request(),
      response.startTimestamp(),
      response.endTimestamp(),
      response.status(),
      response.headers(),
      ByteArrayResponseBody(Base64.getDecoder().decode(response.body().string()), StandardCharsets.UTF_8),
      response.checksums(),
      response.isHttp2
    )
  } else {
    return@transformResponse response
  }
}
//#response-processors
}
}
