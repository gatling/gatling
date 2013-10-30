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
package io.gatling.http.check.checksum

import io.gatling.core.check.{ Check, Extractor }
import io.gatling.core.session.Session
import io.gatling.core.validation.SuccessWrapper
import io.gatling.http.check.{ HttpCheckBuilders, HttpSingleCheckBuilder }
import io.gatling.http.response.Response

object HttpChecksumCheckBuilder {

	def checksum(algorythm: String) = {

		val checksumCheckFactory = (wrapped: Check[Response]) => new ChecksumCheck(algorythm, wrapped)
		val extractor = new Extractor[Response, String] {
			val name = algorythm
			def apply(session: Session, prepared: Response) = prepared.checksum(algorythm).success
		}

		new HttpSingleCheckBuilder[Response, String](
			checksumCheckFactory,
			HttpCheckBuilders.passThroughResponsePreparer,
			extractor)
	}

	val md5 = checksum("MD5")
	val sha1 = checksum("SHA1")
}
