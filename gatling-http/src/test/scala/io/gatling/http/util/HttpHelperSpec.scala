/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpHeaderNames, HttpHeaderValues, HttpResponseStatus }
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class HttpHelperSpec extends AnyFlatSpecLike with Matchers {
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
    HttpHelper.isPermanentRedirect(HttpResponseStatus.MOVED_PERMANENTLY) shouldBe true
  }

  it should "non 301 status code should be recognized as permanent redirect" in {
    HttpHelper.isPermanentRedirect(HttpResponseStatus.SEE_OTHER) shouldBe false
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

  it should "not crash when charset is empty" in {
    HttpHelper.extractCharsetFromContentType("text/plain; charset=") shouldBe None
  }

  "isText" should "detect standard JSON mime type" in {
    val headers = new DefaultHttpHeaders().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
    HttpHelper.isText(headers) shouldBe true
  }

  it should "ignore extra directives" in {
    val headers = new DefaultHttpHeaders().add(HttpHeaderNames.CONTENT_TYPE, s"${HttpHeaderValues.APPLICATION_JSON}; charset = utf-8")
    HttpHelper.isText(headers) shouldBe true
  }

  it should "detect missing content-type headers" in {
    val headers = new DefaultHttpHeaders()
    HttpHelper.isText(headers) shouldBe false
  }

  it should "detect non text mime type" in {
    val headers = new DefaultHttpHeaders().add(HttpHeaderNames.CONTENT_TYPE, "foo")
    HttpHelper.isText(headers) shouldBe false
  }

  it should "detect JSON API mime type" in {
    val headers = new DefaultHttpHeaders().add(HttpHeaderNames.CONTENT_TYPE, "application/vnd.userinfo.v1+json")
    HttpHelper.isText(headers) shouldBe true
  }

  it should "detect SOAP mime type" in {
    val headers = new DefaultHttpHeaders().add(HttpHeaderNames.CONTENT_TYPE, "application/soap+xml")
    HttpHelper.isText(headers) shouldBe true
  }

  it should "detect multipart/related Content-Type with text type attribute" in {
    val headers = new DefaultHttpHeaders().add(
      HttpHeaderNames.CONTENT_TYPE,
      """multipart/related; boundary="----=_Part_512_1387421115.1607689659518"; type="text/xml"; start="1482247947.1607689659518.apache-soap.NCEPRF26WLS01"; charset=utf-8"""
    )
    HttpHelper.isText(headers) shouldBe true
  }
}
