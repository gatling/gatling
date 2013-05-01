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
}