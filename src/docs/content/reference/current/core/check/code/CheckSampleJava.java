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

import io.gatling.javaapi.http.HttpRequestActionBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

class CheckSampleJava {

  static {
//#status-is-200
http("Gatling").get("https://gatling.io")
  .check(status().is(200))
//#status-is-200
;

//#status-is-not-404-or-500
http("Gatling").get("https://gatling.io")
  .check(
    status().not(404),
    status().not(500)
  )
//#status-is-not-404-or-500
;

http("").get("")
//#responseTimeInMillis
.check(responseTimeInMillis().lte(100))
//#responseTimeInMillis

//#bodyString
.check(
  bodyString().is("{\"foo\": \"bar\"}"),
  bodyString().is(ElFileBody("expected-template.json"))
)
//#bodyString

//#bodyBytes
.check(
  bodyBytes().is("{\"foo\": \"bar\"}".getBytes(StandardCharsets.UTF_8)),
  bodyBytes().is(RawFileBody("expected.json"))
)
//#bodyBytes

//#bodyLength
.check(bodyLength().is(1024))
//#bodyLength

//#bodyStream
.check(bodyStream().transform(is -> {
  // decode the Base64 stream into a String
  try (InputStream base64Is = Base64.getDecoder().wrap(is)) {
      return org.apache.commons.io.IOUtils.toString(base64Is, StandardCharsets.UTF_8.name());
  } catch (IOException e) {
      throw new RuntimeException("Impossible to decode Base64 stream");
  }
}))
//#bodyStream

//#substring
.check(
  // with a static value
  // (identical to substring("expected").find().exists())
  substring("expected"),
  // with a Gatling EL
  substring("#{expectedKey}"),
  // with a function
  substring(session -> "expectedValue"),
  substring("Error:").notExists(),
  // this will save a List<Int>
  substring("foo").findAll().saveAs("indices"),
  // this will save the number of occurrences of foo
  substring("foo").count().saveAs("counts")
)
//#substring

//#regex
.check(
  // with a static value without capture groups
  regex("<td class=\"number\">"),
  // with a Gatling EL without capture groups
  regex("<td class=\"number\">ACC#{account_id}</td>"),
  // with a static value with one single capture group
  regex("/private/bank/account/(ACC[0-9]*)/operations.html")
)
//#regex

//#regex-ofType
// In Java, use captureGroups(numberOfCaptureGroups) to capture List<String>
.check(
  regex("foo(.*)bar(.*)baz").captureGroups(2)
)
//#regex-ofType

//#xpath
.check(
  // simple expression for a document that doesn't use namespaces
  xpath("//input[@id='text1']/@value"),
  // mandatory namespaces parameter for a document that uses namespaces
  xpath("//foo:input[@id='text1']/@value", Collections.singletonMap("foo", "http://foo.com"))
)
//#xpath

//#jsonPath
.check(
  // with a static value
  jsonPath("$..foo.bar[2].baz"),
  // with a Gatling EL String
  jsonPath("$..foo.bar[#{index}].baz"),
  // with a function
  jsonPath(session -> "$..foo.bar[" + session.getInt("session") + "].baz")
)
//#jsonPath

//#jsonPath-ofType
.check(
  jsonPath("$.foo").ofString(),
  jsonPath("$.foo").ofBoolean(),
  jsonPath("$.foo").ofInt(),
  jsonPath("$.foo").ofLong(),
  jsonPath("$.foo").ofDouble(),
  // JSON array
  jsonPath("$.foo").ofList(),
  // JSON object
  jsonPath("$.foo").ofMap(),
  // anything
  jsonPath("$.foo").ofObject()
)
//#jsonPath-ofType

//#jsonPath-Int
.check(
  jsonPath("$.foo").ofInt().is(1)
)
//#jsonPath-Int

//#jmesPath
.check(
  // with a static value
  jmesPath("foo.bar[2].baz"),
  // with a Gatling EL String
  jmesPath("foo.bar[#{index}].baz"),
  // with a function
  jmesPath(session -> "foo.bar[" + session.getInt("session") + "].baz")
)
//#jmesPath

//#jmesPath-ofType
.check(
  jmesPath("foo").ofString(),
  jmesPath("foo").ofBoolean(),
  jmesPath("foo").ofInt(),
  jmesPath("foo").ofLong(),
  jmesPath("foo").ofDouble(),
  // JSON array
  jmesPath("foo").ofList(),
  // JSON object
  jmesPath("foo").ofMap(),
  // anything
  jmesPath("foo").ofObject()
)
//#jmesPath-ofType

//#jmesPath-Int
.check(
  jmesPath("foo").ofInt().is(1)
)
//#jmesPath-Int

//#css
.check(
  // with a static value
  css("#foo"),
  // with a Gatling EL String
  css("##{id}"),
  // with a function
  css(session -> "#" + session.getString("id")),
  // with an attribute
  css("article.more a", "href")
)
//#css

//#css-ofType
.check(
  css("article.more a", "href").ofNode()
)
//#css-ofType

//#form
.check(
  form("myForm")
)
//#form

//#checksum
.check(
  md5().is("???"),
  sha1().is("???")
)
//#checksum

//#find
.check(
  // those 2 are identical because jmesPath can only return 1 value
  // so find is better ommitted
  jmesPath("foo"),
  jmesPath("foo").find(),
  // jsonPath can return multiple values
  // those 3 are identical so find is better ommitted
  jsonPath("$.foo"),
  jsonPath("$.foo").find(),
  jsonPath("$.foo").find(0),
  // captures the 2nd occurrence
  jsonPath("$.foo").find(1)
)
//#find

//#findAll
.check(
  jsonPath("$.foo").findAll()
)
//#findAll

//#findRandom
.check(
  // identical to findRandom(1, false)
  jsonPath("$.foo").findRandom(),
  // identical to findRandom(1, false)
  jsonPath("$.foo").findRandom(1),
  // identical to findRandom(3, false)
  // best effort to pick 3 entries, less if not enough
  jsonPath("$.foo").findRandom(3),
  // fail if less than 3 overall captured values
  jsonPath("$.foo").findRandom(3, true)
)
//#findRandom

//#count
.check(
  jsonPath("$.foo").count()
)
//#count

//#withDefault
.check(
  jsonPath("$.foo")
    .withDefault("defaultValue")
)
//#withDefault

//#transform
.check(
  jsonPath("$.foo")
    // append "bar" to the value captured in the previous step
    .transform(string -> string + "bar")
)
//#transform

//#transformWithSession
.check(
  jsonPath("$.foo")
    // append the value of the "bar" attribute
    // to the value captured in the previous step
    .transformWithSession((string, session) -> string + session.getString("bar"))
)
//#transformWithSession

//#transformOption
.check(
  jmesPath("foo")
    // extract can be null
    .transform(extract -> Optional.of(extract).orElse("default"))
)
//#transformOption

//#transformOptionWithSession
.check(
  jmesPath("foo")
    // extract can be null
    .transformWithSession((extract, session) ->
      Optional.of(extract).orElse(session.getString("default"))
    )
)
//#transformOptionWithSession

//#is
.check(
  // with a static value
  jmesPath("foo").is("expected"),
  // with a Gatling EL String (BEWARE DIFFERENT METHOD)
  jmesPath("foo").isEL("#{expected}"),
  // with a function
  jmesPath("foo").is(session -> session.getString("expected"))
)
//#is

//#isNull
.check(
  jmesPath("foo")
    .isNull()
)
//#isNull

//#not
.check(
  // with a static value
  jmesPath("foo").not("unexpected"),
  // with a Gatling EL String (BEWARE DIFFERENT METHOD)
  jmesPath("foo").notEL("#{unexpected}"),
  // with a function
  jmesPath("foo").not(session -> session.getString("unexpected"))
)
//#not

//#notNull
.check(
  jmesPath("foo").notNull()
)
//#notNull

//#exists
.check(
  jmesPath("foo").exists()
)
//#exists

//#notExists
.check(
  jmesPath("foo").notExists()
)
//#notExists

//#in
.check(
  // with a static values varargs
  jmesPath("foo").in("value1", "value2"),
  // with a static values List
  jmesPath("foo").in(Arrays.asList("value1", "value2")),
  // with a Gatling EL String that points to a List in Session (BEWARE DIFFERENT METHOD)
  jmesPath("foo").inEL("#{expectedValues}"),
  // with a function
  jmesPath("foo").in(session -> Arrays.asList("value1", "value2"))
)
//#in

//#validator
.check(
  jmesPath("foo")
    .validate(
      "MyCustomValidator",
      (actual, session) -> {
        String prefix = session.getString("prefix");
        if (actual == null) {
          throw new NullPointerException("Value is missing");
        } else if (!actual.startsWith(prefix)) {
          throw new IllegalArgumentException("Value " + actual + " should start with " + prefix);
        }
        return actual;
      })
)
//#validator

//#name
.check(
  jmesPath("foo").name("My custom error message")
)
//#name

//#saveAs
.check(
  jmesPath("foo").saveAs("key")
)
//#saveAs

//#checkIf
// with a Gatling EL String condition that resolves a Boolean
.checkIf("#{bool}").then(
  jmesPath("foo")
)
// with a function
.checkIf(session -> session.getString("key").equals("executeCheck")).then(
  jmesPath("foo")
)
//#checkIf

//#all-together
.check(
  // check the HTTP status is 200
  status().is(200),

  // check the HTTP is in [200, 210]
  status().in(200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210),

  // check the response body contains 5 https links
  regex("https://(.*)").count().is(5),

  // check the response body contains 2 https links,
  // the first one to www.google.com and the second one to gatling.io
  regex("https://(.*)/.*").findAll().is(Arrays.asList("www.google.com", "gatling.io")),

  // check the response body contains a second occurrence of "someString"
  substring("someString").find(1).exists(),

  // check the response body does not contain "someString"
  substring("someString").notExists()
)
//#all-together
;
  }
}
