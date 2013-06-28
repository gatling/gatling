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
package io.gatling.core.util

import java.io.File
import java.security.MessageDigest

import org.apache.commons.io.FileUtils
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import io.gatling.core.util.StringHelper.RichString

@RunWith(classOf[JUnitRunner])
class StringHelperSpec extends Specification {

	val fileBytes = FileUtils.readFileToByteArray(new File("src/test/resources/emoticon.png"))

	"bytes2Hex" should {

		"correctly compute file sha-1" in {
			val md = MessageDigest.getInstance("SHA-1")
			md.update(fileBytes)
			val digestBytes = md.digest
			StringHelper.bytes2Hex(digestBytes) must beEqualTo("665a5bf97191eb3d8b2a20d833182313343af073")
		}

		"correctly compute file md5" in {
			val md = MessageDigest.getInstance("MD5")
			md.update(fileBytes)
			val digestBytes = md.digest
			StringHelper.bytes2Hex(digestBytes) must beEqualTo("08f6575e7712febe2f529e1ea2c0179e")
		}
	}

	"leftPad" should {
		"pad correctly a two digits number" in {
			"12".leftPad(6) must beEqualTo("    12")
		}

		"not pad when the number of digits is higher than the expected string size" in {
			"123456".leftPad(4) must beEqualTo("123456")
		}
	}

	"rightPad" should {
		"pad correctly a two digits number" in {
			"12".rightPad(6) must beEqualTo("12    ")
		}

		"not pad when the number of digits is higher than the expected string size" in {
			"123456".rightPad(4) must beEqualTo("123456")
		}
	}
}