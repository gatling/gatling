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
package com.excilys.ebi.gatling.http.check.checksum

import com.excilys.ebi.gatling.core.check.{ Check, CheckFactory, Extractor }
import com.excilys.ebi.gatling.core.session.noopStringExpression
import com.excilys.ebi.gatling.core.validation.SuccessWrapper
import com.excilys.ebi.gatling.http.check.{ HttpCheckBuilders, HttpSingleCheckBuilder }
import com.excilys.ebi.gatling.http.response.ExtendedResponse

object HttpChecksumCheckBuilder {

	def checksum(algorythm: String) = {

		val checksumCheckFactory = (wrapped: Check[ExtendedResponse]) => new ChecksumCheck(algorythm, wrapped)
		val extractor = new Extractor[ExtendedResponse, String, String] {
			val name = "algorythm"
			def apply(prepared: ExtendedResponse, criterion: String) = prepared.checksum(algorythm).success
		}

		new HttpChecksumCheckBuilder(checksumCheckFactory, extractor)
	}

	val md5 = checksum("MD5")
	val sha1 = checksum("SHA1")
}

class HttpChecksumCheckBuilder(checksumCheckFactory: CheckFactory[ChecksumCheck, ExtendedResponse], extractor: Extractor[ExtendedResponse, String, String]) extends HttpSingleCheckBuilder[ExtendedResponse, String, String](
	checksumCheckFactory,
	HttpCheckBuilders.noopResponsePreparer,
	extractor,
	noopStringExpression)