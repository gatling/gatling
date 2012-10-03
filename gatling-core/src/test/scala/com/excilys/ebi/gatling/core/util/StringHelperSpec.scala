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
package com.excilys.ebi.gatling.core.util

import java.io.File
import java.security.MessageDigest

import org.apache.commons.io.FileUtils
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StringHelperSpec extends Specification {

	val fileBytes = FileUtils.readFileToByteArray(new File("src/test/resources/gatling-core-1.2.1.pom"))

	"bytes2Hex" should {

		"correctly compute file sha-1" in {
			val md = MessageDigest.getInstance("SHA-1")
			md.update(fileBytes)
			val digestBytes = md.digest
			StringHelper.bytes2Hex(digestBytes) must beEqualTo("aefb180cba67752f54c7eacf45356dd55db4dcc4")
		}

		"correctly compute file md5" in {
			val md = MessageDigest.getInstance("MD5")
			md.update(fileBytes)
			val digestBytes = md.digest
			StringHelper.bytes2Hex(digestBytes) must beEqualTo("694ad6ef693b035d5207506efa2a6d39")
		}
	}
}