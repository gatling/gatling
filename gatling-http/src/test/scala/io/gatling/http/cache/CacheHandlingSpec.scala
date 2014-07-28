/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.cache

import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.mock.MockitoSugar

import com.ning.http.client.{ FluentCaseInsensitiveStringsMap, HttpResponseStatus, RequestBuilder }

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.StandardCharsets
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.response.{ HttpResponse, ResponseBody }

class CacheHandlingSpec extends FlatSpec with Matchers with MockitoSugar {

  // Default config
  GatlingConfiguration.setUpForTest()

  val defaultHttp = HttpProtocol.DefaultHttpProtocol
  val http = defaultHttp.copy(requestPart = defaultHttp.requestPart.copy(cache = true))
  val request = new RequestBuilder().setUrl("http://localhost").build

  def getResponseExpire(headers: Seq[(String, String)]) = {
    val status = mock[HttpResponseStatus]
    val body = mock[ResponseBody]
    val headersMap = new FluentCaseInsensitiveStringsMap
    headers.foreach { case (name, value) => headersMap.add(name, value) }
    val response = HttpResponse(request, Some(status), headersMap, body, Map.empty, 0, StandardCharsets.UTF_8, -1, -1, -1, -1)

    CacheHandling.getResponseExpires(http, response)
  }

  "getResponseExpires()" should "correctly support Pragma header" in {
    getResponseExpire(List(HeaderNames.Pragma -> HeaderValues.NoCache)) shouldBe None
  }

  it should "correctly support Cache-Control header" in {
    getResponseExpire(List(HeaderNames.CacheControl -> "max-age=1")) shouldBe 'defined
    getResponseExpire(List(HeaderNames.CacheControl -> "private, max-age=3600, must-revalidate")) shouldBe 'defined
    getResponseExpire(List(HeaderNames.CacheControl -> "public, no-cache")) shouldBe None
    getResponseExpire(List(HeaderNames.CacheControl -> "public, max-age=-1")) shouldBe None
    getResponseExpire(List(HeaderNames.CacheControl -> "public, max-age=0")) shouldBe None
    getResponseExpire(List(HeaderNames.CacheControl -> HeaderValues.NoStore)) shouldBe None
  }

  it should "correctly support Expires header" in {
    getResponseExpire(List(HeaderNames.Expires -> "Wed, 16 Oct 2033 21:56:44 GMT")) shouldBe 'defined
  }

  it should "Cache-Control has priority over Expires" in {
    getResponseExpire(List(HeaderNames.Expires -> "Tue, 19 Jan 2038 03:14:06 GMT", HeaderNames.CacheControl -> HeaderValues.NoStore)) shouldBe None
    getResponseExpire(List(HeaderNames.Expires -> "Tue, 19 Jan 2038 03:14:06 GMT", HeaderNames.CacheControl -> "max-age=-1")) shouldBe None
    getResponseExpire(List(HeaderNames.Expires -> "Tue, 19 Jan 2038 03:14:06 GMT", HeaderNames.CacheControl -> "max-age=0")) shouldBe None
    getResponseExpire(List(HeaderNames.Expires -> "Tue, 19 Jan 2038 03:14:06 GMT", HeaderNames.CacheControl -> "max-age=567")) shouldBe 'defined
  }

  it should "Pragma has priority over Cache-Control" in {
    getResponseExpire(List(HeaderNames.Pragma -> HeaderValues.NoCache, HeaderNames.CacheControl -> "max-age=3600")) shouldBe None
    getResponseExpire(List(HeaderNames.Pragma -> HeaderValues.NoCache, HeaderNames.Expires -> "3600")) shouldBe None
  }

  "extractExpiresValue()" should "supports Expires field format" in {
    CacheHandling.extractExpiresValue("Thu, 01 Dec 1994 16:00:00 GMT") shouldBe Some(786297600000L)
    CacheHandling.extractExpiresValue("Tue, 19 Jan 2038 03:14:06 GMT") shouldBe Some(2147483646000L)
  }

  it should "defaults to false if it's not Expires field format" in {
    CacheHandling.extractExpiresValue("fail") shouldBe None
  }

  "extractMaxAgeValue()" should "tell if there is a 'max-age' control and gets its value if superior to zero" in {
    CacheHandling.extractMaxAgeValue("public") shouldBe None
    CacheHandling.extractMaxAgeValue("private, max-age=3600, must-revalidate") shouldBe Some(3600)
    CacheHandling.extractMaxAgeValue("private, max-age=nicolas, must-revalidate") shouldBe None
    CacheHandling.extractMaxAgeValue("private, max-age=0, must-revalidate") shouldBe Some(0)
    CacheHandling.extractMaxAgeValue("max-age=-1") shouldBe Some(-1)
    CacheHandling.extractMaxAgeValue("max-age=-123") shouldBe Some(-1)
    CacheHandling.extractMaxAgeValue("max-age=5") shouldBe Some(5)
    CacheHandling.extractMaxAgeValue("max-age=567") shouldBe Some(567)
  }
}
