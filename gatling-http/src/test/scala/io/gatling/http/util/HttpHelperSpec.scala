/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.http.util

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HttpHelperSpec extends Specification {

	"parseFormBody" should {

		"support unique param" in {
			HttpHelper.parseFormBody("foo=bar") must beEqualTo(List("foo" -> "bar"))
		}

		"support multiple params" in {
			HttpHelper.parseFormBody("foo=bar&baz=qux") must beEqualTo(List("foo" -> "bar", "baz" -> "qux"))
		}

		"support empty value param" in {
			HttpHelper.parseFormBody("foo=&baz=qux") must beEqualTo(List("foo" -> "", "baz" -> "qux"))
		}
	}

	"computeRedirectUrl" should {

		"properly handle an absolute location with query params" in {
			HttpHelper.computeRedirectUrl("http://foo.com/bar?baz=qux", "http://foo.com") must beEqualTo("http://foo.com/bar?baz=qux")
		}

		"properly handle a relative root location with query params" in {
			HttpHelper.computeRedirectUrl("/bar?baz=qux", "http://foo.com") must beEqualTo("http://foo.com/bar?baz=qux")
		}

		"properly handle a relative non root location with query params" in {
			HttpHelper.computeRedirectUrl("bar?baz=qux", "http://foo.com/aaa/bbb") must beEqualTo("http://foo.com/aaa/bar?baz=qux")
		}

		"not encode query" in {
			HttpHelper.computeRedirectUrl("/baz?qix:a=b", "https://foo.bar/") must beEqualTo("https://foo.bar/baz?qix:a=b")
		}
	}
}