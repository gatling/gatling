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

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

class HandlingJsfSampleJava {

  //#factory-methods
  private static final CheckBuilder jsfViewStateCheck =
    css("input[name='javax.faces.ViewState']", "value").saveAs("viewState");

  private static final CheckBuilder jsfPartialViewStateCheck =
    xpath("//update[contains(@id,'ViewState')]").saveAs("viewState");

  public static final HttpRequestActionBuilder jsfGet(String name, String url) {
    return http(name).get(url)
      .check(jsfViewStateCheck);
  }

  public static final HttpRequestActionBuilder jsfPost(String name, String url) {
    return http(name).post(url)
      .formParam("javax.faces.ViewState", "#{viewState}")
      .check(jsfViewStateCheck);
  }

  public static final HttpRequestActionBuilder jsfPartialPost(String name, String url) {
    return http(name)
      .post(url)
      .header("Faces-Request", "partial/ajax")
      .formParam("javax.faces.partial.ajax", "true")
      .formParam("javax.faces.ViewState", "#{viewState}")
      .check(jsfPartialViewStateCheck);
  }
  //#factory-methods

  //#example-scenario
  ScenarioBuilder scn = scenario("Scenario Name")
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
    );
  //#example-scenario

  private static final class Trinidad {
    //#trinidad
    private static final CheckBuilder jsfPageFlowCheck =
      regex("\\?_afPfm=([^\"]*)").saveAs("afPfm");
    private static final CheckBuilder jsfViewStateCheck =
      regex("=\"javax.faces.ViewState\" value=\"([^\"]*)").saveAs("viewState");

    public static final HttpRequestActionBuilder jsfGet(String name, String url) {
      return http(name).get(url)
        .check(jsfViewStateCheck);
    }
    public static final HttpRequestActionBuilder jsfPost(String name, String url) {
      return http(name).post(url)
        .formParam("javax.faces.ViewState", "#{viewState}")
        .check(jsfViewStateCheck).check(jsfPageFlowCheck);
    }

    public static final HttpRequestActionBuilder trinidadPost(String name, String url) {
      return http(name).post(url)
        .formParam("javax.faces.ViewState", "#{viewState}")
        .queryParam("_afPfm", "#{afPfm}")
        .check(jsfViewStateCheck)
        .check(jsfPageFlowCheck);
    }
    public static final HttpRequestActionBuilder trinidadDownload(String name, String url) {
      return http(name).post(url)
        .formParam("javax.faces.ViewState", "#{viewState}")
        .queryParam("_afPfm", "#{afPfm}");
    }
    //#trinidad
  }
}
