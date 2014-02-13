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

import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.ning.http.client.{ FluentCaseInsensitiveStringsMap, HttpResponseStatus, RequestBuilder }

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.util.StandardCharsets
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.response.{ HttpResponse, ResponseBody }

@RunWith(classOf[JUnitRunner])
class CacheHandlingSpec extends Specification with Mockito {

	// Default config
	GatlingConfiguration.setUp()

	"getResponseExpires()" should {

		val defaultHttp = HttpProtocol.default
		val http = defaultHttp.copy(requestPart = defaultHttp.requestPart.copy(cache = true))
		val request = new RequestBuilder().setUrl("http://localhost").build

		def getResponseExpire(headers: Seq[(String, String)]) = {
			val status = mock[HttpResponseStatus].smart
			val body = mock[ResponseBody].smart
			val headersMap = new FluentCaseInsensitiveStringsMap
			headers.foreach { case (name, value) => headersMap.add(name, value) }
			val response = HttpResponse(request, Some(status), headersMap, body, Map.empty, 0, StandardCharsets.UTF_8, -1, -1, -1, -1)

			CacheHandling.getResponseExpires(http, response)
		}

		"correctly support Pragma header" in {
			getResponseExpire(List(HeaderNames.PRAGMA -> HeaderValues.NO_CACHE)) must beNone
		}

		"correctly support Cache-Control header" in {
			getResponseExpire(List(HeaderNames.CACHE_CONTROL -> "max-age=1")) must beSome
			getResponseExpire(List(HeaderNames.CACHE_CONTROL -> "private, max-age=3600, must-revalidate")) must beSome
			getResponseExpire(List(HeaderNames.CACHE_CONTROL -> "public, no-cache")) must beNone
			getResponseExpire(List(HeaderNames.CACHE_CONTROL -> "public, max-age=-1")) must beNone
			getResponseExpire(List(HeaderNames.CACHE_CONTROL -> "public, max-age=0")) must beNone
			getResponseExpire(List(HeaderNames.CACHE_CONTROL -> "no-store")) must beNone
		}

		"correctly support Expires header" in {
			getResponseExpire(List(HeaderNames.EXPIRES -> "Wed, 16 Oct 2033 21:56:44 GMT")) must beSome
		}

		"Cache-Control has priority over Expires" in {
			getResponseExpire(List(HeaderNames.EXPIRES -> "Tue, 19 Jan 2038 03:14:06 GMT", HeaderNames.CACHE_CONTROL -> "no-store")) must beNone
			getResponseExpire(List(HeaderNames.EXPIRES -> "Tue, 19 Jan 2038 03:14:06 GMT", HeaderNames.CACHE_CONTROL -> "max-age=-1")) must beNone
			getResponseExpire(List(HeaderNames.EXPIRES -> "Tue, 19 Jan 2038 03:14:06 GMT", HeaderNames.CACHE_CONTROL -> "max-age=0")) must beNone
			getResponseExpire(List(HeaderNames.EXPIRES -> "Tue, 19 Jan 2038 03:14:06 GMT", HeaderNames.CACHE_CONTROL -> "max-age=567")) must beSome
		}

		"Pragma has priority over Cache-Control" in {
			getResponseExpire(List(HeaderNames.PRAGMA -> "no-cache", HeaderNames.CACHE_CONTROL -> "max-age=3600")) must beNone
			getResponseExpire(List(HeaderNames.PRAGMA -> "no-cache", HeaderNames.EXPIRES -> "3600")) must beNone
		}
	}

	"extractExpiresValue()" should {

		"supports Expires field format" in {
			CacheHandling.extractExpiresValue("Thu, 01 Dec 1994 16:00:00 GMT") must beSome(786297600000L)
			CacheHandling.extractExpiresValue("Tue, 19 Jan 2038 03:14:06 GMT") must beSome(2147483646000L)
		}

		"defaults to false if it's not Expires field format" in {
			CacheHandling.extractExpiresValue("fail") must beNone
		}
	}

	"extractMaxAgeValue()" should {

		"tell if there is a 'max-age' control and gets its value if superior to zero" in {
			CacheHandling.extractMaxAgeValue("public") must beNone
			CacheHandling.extractMaxAgeValue("private, max-age=3600, must-revalidate") must beSome(3600)
			CacheHandling.extractMaxAgeValue("private, max-age=nicolas, must-revalidate") must beNone
			CacheHandling.extractMaxAgeValue("private, max-age=0, must-revalidate") must beSome(0)
			CacheHandling.extractMaxAgeValue("max-age=-1") must beSome(-1)
			CacheHandling.extractMaxAgeValue("max-age=-123") must beSome(-1)
			CacheHandling.extractMaxAgeValue("max-age=5") must beSome(5)
			CacheHandling.extractMaxAgeValue("max-age=567") must beSome(567)
		}
	}
}
