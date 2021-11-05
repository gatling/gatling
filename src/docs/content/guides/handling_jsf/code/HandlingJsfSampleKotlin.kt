/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import io.gatling.javaapi.core.*
import io.gatling.javaapi.http.*

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*
import java.time.Duration

class HandlingJsfSampleKotlin {

//#factory-methods
val jsfViewStateCheck =
  css("input[name='javax.faces.ViewState']", "value").saveAs("viewState")

val jsfPartialViewStateCheck =
  xpath("//update[contains(@id,'ViewState')]").saveAs("viewState")

fun jsfGet(name: String, url: String): HttpRequestActionBuilder {
  return http(name).get(url)
    .check(jsfViewStateCheck)
}

fun jsfPost(name: String, url: String): HttpRequestActionBuilder {
  return http(name).post(url)
    .formParam("javax.faces.ViewState", "#{viewState}")
    .check(jsfViewStateCheck)
}

fun jsfPartialPost(name: String, url: String): HttpRequestActionBuilder {
  return http(name)
    .post(url)
    .header("Faces-Request", "partial/ajax")
    .formParam("javax.faces.partial.ajax", "true")
    .formParam("javax.faces.ViewState", "#{viewState}")
    .check(jsfPartialViewStateCheck)
}
//#factory-methods

//#example-scenario
val scn = scenario("Scenario Name")
  .exec(jsfGet("request_1", "/showcase-labs/ui/pprUpdate.jsf"))
  .pause(Duration.ofMillis(80))
  .exec(
    jsfPost("request_2", "/showcase-labs/ui/pprUpdate.jsf")
      .formParam("javax.faces.partial.ajax", "true")
      .formParam("javax.faces.source", "form:btn")
      .formParam("javax.faces.partial.execute", "@all")
      .formParam("javax.faces.partial.render", "form:display")
      .formParam("form:btn", "form:btn")
      .formParam("form", "form")
      .formParam("form:name", "foo")
  )
//#example-scenario

  object Trinidad {
//#trinidad
val jsfPageFlowCheck =
  regex("\\?_afPfm=([^\"]*)").saveAs("afPfm")
val jsfViewStateCheck =
  regex("=\"javax.faces.ViewState\" value=\"([^\"]*)").saveAs("viewState")

fun jsfGet(name: String, url: String): HttpRequestActionBuilder {
  return http(name).get(url)
    .check(jsfViewStateCheck)
}
fun jsfPost(name: String, url: String): HttpRequestActionBuilder {
  return http(name).post(url)
    .formParam("javax.faces.ViewState", "#{viewState}")
    .check(jsfViewStateCheck).check(jsfPageFlowCheck)
}

fun trinidadPost(name: String, url: String): HttpRequestActionBuilder {
  return http(name).post(url)
    .formParam("javax.faces.ViewState", "#{viewState}")
    .queryParam("_afPfm", "#{afPfm}")
    .check(jsfViewStateCheck)
    .check(jsfPageFlowCheck)
}
fun trinidadDownload(name: String, url: String): HttpRequestActionBuilder {
  return http(name).post(url)
    .formParam("javax.faces.ViewState", "#{viewState}")
    .queryParam("_afPfm", "#{afPfm}")
}
//#trinidad
  }
}
