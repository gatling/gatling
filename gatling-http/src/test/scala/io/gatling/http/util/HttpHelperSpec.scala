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
package io.gatling.http.util

import java.nio.charset.StandardCharsets.UTF_8

import io.gatling.BaseSpec

class HttpHelperSpec extends BaseSpec {

  "parseFormBody" should "support unique param" in {
    HttpHelper.parseFormBody("foo=bar") shouldBe List("foo" -> "bar")
  }

  it should "support multiple params" in {
    HttpHelper.parseFormBody("foo=bar&baz=qux") shouldBe List("foo" -> "bar", "baz" -> "qux")
  }

  it should "support empty value param" in {
    HttpHelper.parseFormBody("foo=&baz=qux") shouldBe List("foo" -> "", "baz" -> "qux")
  }

  it should "recognize 301 status code as permanent redirect" in {
    HttpHelper.isPermanentRedirect(301) shouldBe true
  }

  it should "non 301 status code should be recognized as permanent redirect" in {
    HttpHelper.isPermanentRedirect(303) shouldBe false
  }

  "extractCharsetFromContentType" should "extract charset when it exists in latest position" in {
    HttpHelper.extractCharsetFromContentType("text/plain; charset=UTF-8") shouldBe Some(UTF_8)
  }

  it should "extract charset when it exists in latest position, whatever the case" in {
    HttpHelper.extractCharsetFromContentType("text/plain; charset=utf-8") shouldBe Some(UTF_8)
  }

  it should "extract charset when it exists in middle position" in {
    HttpHelper.extractCharsetFromContentType("text/plain; charset=UTF-8; foo=bar") shouldBe Some(UTF_8)
  }

  it should "extract charset when it exists with leading and trailing spaces" in {
    HttpHelper.extractCharsetFromContentType("text/plain; charset= UTF-8 ; foo=bar") shouldBe Some(UTF_8)
  }

  it should "extract charset when it's wrapped in double quotes" in {
    HttpHelper.extractCharsetFromContentType("""text/plain;  charset="UTF-8" ; foo=bar""") shouldBe Some(UTF_8)
  }

  it should "not crash when double quotes are unbalanced" in {
    HttpHelper.extractCharsetFromContentType("""text/plain;  charset="UTF-8 ; foo=bar""") shouldBe Some(UTF_8)
    HttpHelper.extractCharsetFromContentType("""text/plain;  charset=UTF-8" ; foo=bar""") shouldBe Some(UTF_8)
  }

  it should "not crash when charset is unknown" in {
    HttpHelper.extractCharsetFromContentType("text/plain; charset=Foo") shouldBe None
  }
}
