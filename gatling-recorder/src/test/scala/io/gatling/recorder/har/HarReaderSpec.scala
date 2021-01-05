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

package io.gatling.recorder.har

import java.nio.charset.StandardCharsets.UTF_8

import scala.util.Using

import io.gatling.BaseSpec
import io.gatling.core.filter.Filters

import io.netty.handler.codec.http.{ HttpHeaderNames, HttpHeaderValues }

class HarReaderSpec extends BaseSpec {

  private def readHar(file: String, filters: Option[Filters]): Seq[HttpTransaction] =
    Using.resource(Thread.currentThread.getContextClassLoader.getResourceAsStream("har/" + file)) { is =>
      HarReader.readStream(is, filters)
    }

  private def testGet(file: String): Unit = {
    val transactions = readHar(file, None)
    transactions should have size 1
    val getTransaction = transactions.head

    getTransaction.request.httpVersion shouldBe "HTTP/1.1"
    getTransaction.request.method shouldBe "GET"
    getTransaction.request.uri shouldBe "http://computer-database.gatling.io/computers?p=1"
    getTransaction.request.headers.get(HttpHeaderNames.HOST) shouldBe "computer-database.gatling.io"
    getTransaction.request.headers.get(HttpHeaderNames.USER_AGENT).nonEmpty shouldBe true
    getTransaction.request.headers.get(HttpHeaderNames.ACCEPT_ENCODING) shouldBe "gzip, deflate"
    getTransaction.request.headers.get(HttpHeaderNames.CONNECTION) shouldBe "keep-alive"
    getTransaction.request.body shouldBe empty

    getTransaction.response.status shouldBe 200
    getTransaction.response.statusText shouldBe "OK"
    getTransaction.response.headers.get(HttpHeaderNames.CONTENT_TYPE) shouldBe "text/html; charset=utf-8"
    getTransaction.response.headers.get(HttpHeaderNames.CONTENT_LENGTH).toInt shouldBe 7281
    getTransaction.response.body should have length 7256

    getTransaction.response.timestamp shouldBe >(getTransaction.request.timestamp)
  }

  "Parsing a GET" should "work with Chrome 61" in {
    testGet("chrome61/get.har")
  }

  it should "work with FireFox 56" in {
    testGet("firefox56/get.har")
  }

  it should "work with Charles 4.2" in {
    testGet("charles42/get.har")
  }

  private def testForm(file: String): Unit = {
    val transactions = readHar(file, None)
    transactions should have size 2

    val getTransaction = transactions.head
    getTransaction.request.method shouldBe "GET"

    val postTransaction = transactions(1)
    postTransaction.request.method shouldBe "POST"
    postTransaction.request.headers.get(HttpHeaderNames.CONTENT_TYPE) shouldBe HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString
    new String(postTransaction.request.body, UTF_8) shouldBe "name=NAME&quest=QUEST&color=chartreuse&swallow=african&text=HI"
  }

  "Parsing a form post" should "work with Chrome 61" in {
    testForm("chrome61/form.har")
  }

  it should "work with FireFox 56" in {
    testForm("firefox56/form.har")
  }

  it should "work with Charles 4.2" in {
    testForm("charles42/form.har")
  }

  private def testMultipart(file: String): Unit = {
    val transactions = readHar(file, None)
    transactions should have size 2

    val getTransaction = transactions.head
    getTransaction.request.method shouldBe "GET"

    val postTransaction = transactions(1)
    postTransaction.request.method shouldBe "POST"
    postTransaction.request.headers.get(HttpHeaderNames.CONTENT_TYPE) should startWith(HttpHeaderValues.MULTIPART_FORM_DATA.toString)
    if (!file.contains("chrome") && !file.contains("charles")) {
      // FIXME https://bugs.chromium.org/p/chromium/issues/detail?id=766715#c4
      // FIXME Charles parses multipart into params and we don't support that yet
      postTransaction.request.body.length shouldBe >(0)
    }
  }

  "Parsing a multipart post" should "work with Chrome 61" in {
    testMultipart("chrome61/multipart.har")
  }

  it should "work with FireFox 56" in {
    testMultipart("firefox56/multipart.har")
  }

  it should "work with Charles 4.2" in {
    testMultipart("charles42/multipart.har")
  }

  private def testRedirectAfterPost(file: String): Unit = {
    val transactions = readHar(file, None)
    transactions should have size 2
    val postTransaction = transactions.head
    postTransaction.request.method shouldBe "POST"
    postTransaction.response.headers.get(HttpHeaderNames.LOCATION) shouldBe "/computers"
    postTransaction.response.body shouldBe empty

    val redirectGetTransaction = transactions(1)
    redirectGetTransaction.request.uri shouldBe "http://computer-database.gatling.io/computers"
    redirectGetTransaction.request.method shouldBe "GET"
    redirectGetTransaction.request.body shouldBe empty
  }

  "Parsing a redirect after post" should "work with Chrome 61" in {
    testRedirectAfterPost("chrome61/redirect-post.har")
  }

  it should "work with FireFox 56" in {
    testRedirectAfterPost("firefox56/redirect-post.har")
  }

  it should "work with Charles 4.2" in {
    testRedirectAfterPost("charles42/redirect-post.har")
  }
}
