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
package io.gatling.core.util

import org.junit.runner.RunWith
import io.gatling.core.test.ValidationSpecification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UnsyncByteArrayInputStreamSpec extends ValidationSpecification {

	"BytesInputStream" should {

		val bytes = "test string".getBytes("utf-8")

		"signal eof when all bytes are read" in {
			val byteStream = new UnsyncByteArrayInputStream(bytes)
			byteStream.read(bytes, 0, bytes.length)
			byteStream.read(bytes, 0, 1) should beEqualTo(-1)
		}

		"not allow to read more than available bytes" in {
			val byteStream = new UnsyncByteArrayInputStream(bytes)
			byteStream.read(bytes, 0, bytes.length + 1) should beEqualTo(bytes.length)
		}
	}
}
