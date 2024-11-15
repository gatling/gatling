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

//#requestName
// with a static value
http("requestName").get("https://gatling.io");
// with a dynamic value computed from a Gatling Expression Language String
http("#{requestName}").get("https://gatling.io");
// a dynamic value computed from a function
http((session) => session.get("requestName")).get("https://gatling.io");
//#requestName

//#inline
// inline style
scenario("scenarioName")
  .exec(http("requestName").get("url"));

// non inline style
const request = http("RequestName").get("url");

scenario("MyScenario")
  .exec(request);
//#inline

//#methods
// with an absolute static url
http("name").get("https://gatling.io");
// with a dynamic value computed from a Gatling Expression Language String
http("name").get("#{url}");
// a dynamic value computed from a function
http("name").get((session) => session.get("url"));

http("name").put("https://gatling.io");
http("name").post("https://gatling.io");
http("name").delete("https://gatling.io");
http("name").head("https://gatling.io");
http("name").patch("https://gatling.io");
http("name").options("https://gatling.io");
http("name").httpRequest("PURGE", "http://myNginx.com");
//#methods

//#full-query-in-url
http("Issues")
  .get("https://github.com/gatling/gatling/issues?milestone=1&state=open");
//#full-query-in-url

//#queryParam
// with static values
http("Issues").get("https://github.com/gatling/gatling/issues")
  .queryParam("milestone", "1")
  .queryParam("state", "open");

// with Gatling EL strings
http("Issues").get("https://github.com/gatling/gatling/issues")
  .queryParam("milestone", "#{milestoneValue}")
  .queryParam("state", "#{stateValue}");

// with functions
http("Issues").get("https://github.com/gatling/gatling/issues")
  .queryParam("milestone", (session) => session.get("milestoneValue"))
  .queryParam("state", (session) => session.get("stateValue"));
//#queryParam


//#multivaluedQueryParam
http("name").get("/")
  // with static values
  .multivaluedQueryParam("param", ["value1", "value2"]);

http("name").get("/")
  // with a Gatling EL string pointing to a List
  .multivaluedQueryParam("param", "#{values}");

http("name").get("/")
  // with a function
  .multivaluedQueryParam("param", (session) => ["value1", "value2"]);
//#multivaluedQueryParam

//#queryParam-multiple
// queryParamSeq isn't implemented in Gatling JS, use queryParamMap:

const params = {
  "key1": "value1",
  "key2": "value2"
};

http("name").get("/")
  .queryParamMap(params);
//#queryParam-multiple

//#headers
// Extracting a map of headers allows you to reuse these in several requests
const sentHeaders = {
  "content-type": "application/javascript",
  "accept": "text/html"
}

http("name").get("/")
  // Adds several headers at once
  .headers(sentHeaders)
  // Adds another header to the request
  .header("keep-alive", "150")
  // Overrides the content-type header
  .header("content-type", "application/json");
//#headers

//#asXXX
// asJson
http("name").post("/")
  .asJson();
// is a shortcut for:
http("name").post("/")
  .header("accept", "application/json")
  // only for requests that have a body
  .header("content-type", "application/json");

// asXml
http("name").post("/")
  .asXml();
// is a shortcut for:
http("name").post("/")
  .header("accept", "application/xhtml+xml")
  // only for requests that have a body
  .header("content-type", "application/xhtml+xml");

// asFormUrlEncoded
http("name").post("/")
  .asFormUrlEncoded();
// is a shortcut for:
http("name").post("/")
  // only for requests that have a body
  .header("content-type", "application/application/x-www-form-urlencoded");

// asMultipartForm
http("name").post("/")
  .asMultipartForm();
// is a shortcut for:
http("name").post("/")
  // only for requests that have a body
  .header("content-type", "multipart/form-data");
//#asXXX

//#ignoreProtocolHeaders
http("name").get("/")
  .ignoreProtocolHeaders();
//#ignoreProtocolHeaders

//#check
http("name").get("/")
  .check(status().is(200));
//#check

//#ignoreProtocolChecks
http("name").get("/")
  .ignoreProtocolChecks();
//#ignoreProtocolChecks

//#StringBody
// with a static payload
http("name").post("/")
  .body(StringBody("{ \"foo\": \"staticValue\" }"));

// with a Gatling EL string payload
http("name").post("/")
  .body(StringBody("{ \"foo\": \"#{dynamicValue}\" }"));

// with a function payload
http("name").post("/")
  .body(StringBody((session) => `{ "foo": "${session.get("dynamicValueKey")}" }`));
//#StringBody

//#template
const template = (session) => {
  const foo = session.get("foo");
  const bar = session.get("bar");
  return `"{ "foo": "${foo}", "bar": "${bar}" }`;
};
//#template

//#template-usage
http("name").post("/")
  .body(StringBody(template));
//#template-usage

//#RawFileBody
// with a static path
http("name").post("/")
  .body(RawFileBody("rawPayload.json"));

// with a Gatling EL String path
http("name").post("/")
  .body(RawFileBody("#{payloadPath}"));

// with a function path
http("name").post("/")
  .body(RawFileBody((session) => session.get("payloadPath")));
//#RawFileBody

//#ElFileBody
http("name").post("/")
  .body(ElFileBody("rawPayload.json"));

// with a Gatling EL String path
http("name").post("/")
  .body(ElFileBody("#{payloadPath}"));

// with a function path
http("name").post("/")
  .body(ElFileBody((session) => session.get("payloadPath")));
//#ElFileBody

//#PebbleStringBody
http("name").post("/")
  .body(PebbleStringBody("{ \"foo\": \"{% if someCondition %}{{someValue}}{% endif %}\" }"));
//#PebbleStringBody

//#PebbleFileBody
// with a static value path
http("name").post("/")
  .body(PebbleFileBody("pebbleTemplate.json"));

// with a Gatling EL string path
http("name").post("/")
  .body(PebbleFileBody("#{templatePath}"));

// with a function path
http("name").post("/")
  .body(PebbleFileBody((session) => session.get("templatePath")));
//#PebbleFileBody

//#ByteArrayBody
// with a static value
http("name").post("/")
  .body(ByteArrayBody([ 0, 1, 5, 4 ]));

// with a static value
http("name").post("/")
  .body(ByteArrayBody("#{bytes}"));

// with a function
http("name").post("/")
  .body(ByteArrayBody((session) => {
    // requires the "base-64" package
    const encoded = base64.encode(session.get("data"));
    const bytes = [];
    for (let i = 0; i < encoded.length; i++) {
      bytes.push(encoded.charCodeAt(i));
    }
    return bytes;
  }));
//#ByteArrayBody

/*
//#InputStreamBody
NOT SUPPORTED
//#InputStreamBody
*/

//#formParam
// with static values
http("name").post("/")
  .formParam("milestone", "1")
  .formParam("state", "open");

// with Gatling EL strings
http("name").post("/")
  .formParam("milestone", "#{milestoneValue}")
  .formParam("state", "#{stateValue}");

// with functions
http("name").post("/")
  .formParam("milestone", (session) => session.get("milestoneValue"))
  .formParam("state", (session) => session.get("stateValue"));
//#formParam

//#multivaluedFormParam
http("name").post("/")
  // with static values
  .multivaluedFormParam("param", ["value1", "value2"]);

http("name").post("/")
  // with a Gatling EL string pointing to a List
  .multivaluedFormParam("param", "#{values}");

http("name").post("/")
  // with a function
  .multivaluedFormParam("param", (session) => ["value1", "value2"]);
//#multivaluedFormParam

//#formParam-multiple
// formParamSeq isn't implemented in Gatling JS, use formParamMap:

const params = {
  "key1": "value1",
  "key2": "value2"
};

http("name").post("/")
  .formParamMap(params);
//#formParam-multiple

//#formFull
http("name").post("/")
  .form("#{previouslyCapturedForm}")
  // override an input
  .formParam("fieldToOverride", "newValue");
//#formFull

//#formUpload
// with a static filepath value
http("name").post("/")
  .formParam("key", "value")
  .formUpload("file1", "file1.dat")
  // you can set multiple files
  .formUpload("file2", "file2.dat");

// with a Gatling EL string filepath
http("name").post("/")
  .formUpload("file1", "#{file1Path}");

// with a function filepath
http("name").post("/")
  .formUpload("file1", (session) => session.get("file1Path"));
//#formUpload

//#bodyPart
// set a single part
http("name").post("/")
  .bodyPart(
    StringBodyPart("partName", "value")
  );

// set a multiple parts
http("name").post("/")
  .bodyParts(
    StringBodyPart("partName1", "value"),
    StringBodyPart("partName2", "value")
  );
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
  );
//#bodyPart-options

/*
//#processRequestBody
NOT SUPPORTED
//#processRequestBody
*/

//#resources
http("name").post("/")
  .resources(
    http("api.js").get("/assets/api.js"),
    http("ga.js").get("/ga.js")
  );
//#resources

//#requestTimeout
http("name").get("/")
  .requestTimeout({ amount: 3, unit: "minutes" });
//#requestTimeout

//#silent
http("name").get("/")
  .silent();
//#silent

http("name").get("/")
//#notSilent
.resources(
  http("resource").get("/assets/images/img1.png")
    .notSilent()
);
//#notSilent

//#resp-processors-imports
//#resp-processors-imports

/*
//#response-processors
NOT SUPPORTED
//#response-processors
*/
