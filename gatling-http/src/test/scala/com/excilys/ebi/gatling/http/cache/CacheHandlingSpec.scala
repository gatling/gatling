/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.http.cache

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import java.text.SimpleDateFormat
import java.util.Date

@RunWith(classOf[JUnitRunner])
class CacheHandlingSpec extends Specification {

	"convertExpireField" should {
		"support format EEEE, dd-MMM-yy HH:mm:ss zzz" in {
			CacheHandling.convertExpireField("Thursday, 09 Aug 2007 05:22:55 GMT") must beEqualTo(1186636975000L)
		}

		"support format EEE, dd MMM yyyy HH:mm:ss zzz" in {
			CacheHandling.convertExpireField("Thu, 09 Aug 2007 05:22:55 GMT") must beEqualTo(1186636975000L)
		}

		"support crapy Int format" in {
			CacheHandling.isFutureExpire("0") must beEqualTo(false)
		}
	}
}