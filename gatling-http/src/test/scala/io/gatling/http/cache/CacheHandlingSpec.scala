/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
import com.ning.http.client.{ RequestBuilder, Response => AHCResponse }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Session
import io.gatling.http.{ HeaderNames, HeaderValues }
import io.gatling.http.config.HttpProtocol
import io.gatling.http.response.HttpResponse
import io.gatling.core.config.ProtocolRegistry

@RunWith(classOf[JUnitRunner])
class CacheHandlingSpec extends Specification with Mockito {

	// Default config
	GatlingConfiguration.setUp()

	"isResponseCacheable()" should {

		val http = HttpProtocol.default.copy(cache = true)
		val request = new RequestBuilder().setUrl("http://localhost").build

		def isCacheable(headers: Seq[(String, String)]) = {
			val ahcResponse = mock[AHCResponse].smart
			headers.foreach { case (name, value) => ahcResponse.getHeader(name) returns value }
			val response = HttpResponse(request, Some(ahcResponse), Map.empty, -1, -1, -1, -1, Array.empty)

			CacheHandling.isResponseCacheable(http, response)
		}

		"correctly support Expires header" in {
			isCacheable(List(HeaderNames.EXPIRES -> "3600")) must beTrue
		}

		"correctly support Pragma header" in {
			isCacheable(List(HeaderNames.EXPIRES -> "3600", HeaderNames.PRAGMA -> HeaderValues.NO_CACHE)) must beFalse
		}

		"correctly support Cache-Control header" in {
			isCacheable(List(HeaderNames.CACHE_CONTROL -> "private, max-age=3600, must-revalidate")) must beTrue
			isCacheable(List(HeaderNames.EXPIRES -> "3600", HeaderNames.CACHE_CONTROL -> "public, no-cache")) must beFalse
			isCacheable(List(HeaderNames.EXPIRES -> "3600", HeaderNames.CACHE_CONTROL -> "public, max-age=0")) must beFalse
			isCacheable(List(HeaderNames.EXPIRES -> "3600", HeaderNames.CACHE_CONTROL -> "no-store")) must beFalse
		}
	}

	"convertExpireField()" should {

		"supports Expires field format" in {
			CacheHandling.isFutureExpire("Thu, 01 Dec 1994 16:00:00 GMT") must beFalse
			CacheHandling.isFutureExpire("Tue, 19 Jan 2038 03:14:06 GMT") must beTrue
		}

		"supports Int format" in {
			CacheHandling.isFutureExpire("0") must beFalse
			CacheHandling.isFutureExpire(Int.MaxValue.toString) must beTrue
		}

		"defaults to false if it's not Expires field format nor Int format" in {
			CacheHandling.isFutureExpire("fail") must beFalse
		}
	}

	"hasPositiveMaxAge()" should {

		"tell if there is a 'max-age' control and if it's value is superior to zero" in {

			CacheHandling.hasPositiveMaxAge("private, max-age=3600, must-revalidate") must beTrue
			CacheHandling.hasPositiveMaxAge("public") must beFalse
			CacheHandling.hasPositiveMaxAge("private, max-age=nicolas, must-revalidate") must beFalse
			CacheHandling.hasPositiveMaxAge("private, max-age=0, must-revalidate") must beFalse
		}
	}
}
