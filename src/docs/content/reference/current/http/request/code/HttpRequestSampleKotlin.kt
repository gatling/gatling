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

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.http.HttpDsl.*
import java.util.*

class HttpRequestSampleKotlin {

init {
//#requestName
// with a static vale
http("requestName").get("https://gatling.io")
// with a static vale
http("#{requestName}").get("https://gatling.io")
// with a static vale
http { session: Session -> session.getString("requestName") }.get("https://gatling.io")
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
http("Issues").get("https://github.com/gatling/gatling/issues?milestone=1&state=open")
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
http("name").get("/") // with static values
  .multivaluedQueryParam("param", listOf("value1", "value2"))

http("name").get("/") // with a Gatling EL string pointing to a List
  .multivaluedQueryParam("param", "#{values}")

http("name").get("/") // with a function
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
  "Content-Type" to "application/javascript",
  "Accept" to "text/html"
)

// Add several headers at once
http("name").get("/")
  .headers(sentHeaders)
  .header("Keep-Alive", "150") // Overrides the Content-Type header
  .header("Content-Type", "application/json")
//#headers

//#asXXX
// asJson
http("name").post("/")
  .asJson()
// is a shortcut for:
http("name").post("/")
  .header("Accept", "application/json")
  .header("Content-Type", "application/json")

// asXml
http("name").post("/")
  .asXml()
// is a shortcut for:
http("name").post("/")
  .header("Accept", "application/xhtml+xml")
  .header("Content-Type", "application/xhtml+xml")

// asFormUrlEncoded
http("name").post("/")
  .asFormUrlEncoded()
// is a shortcut for:
http("name").post("/")
  .header("Content-Type", "application/application/x-www-form-urlencoded")

// asMultipartForm
http("name").post("/")
  .asMultipartForm()
// is a shortcut for:
http("name").post("/")
  .header("Content-Type", "multipart/form-data")
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
  .body(StringBody { session: Session -> """{ "foo": "${session.getString("dynamicValueKey")}" }""" })
//#StringBody
}
}
