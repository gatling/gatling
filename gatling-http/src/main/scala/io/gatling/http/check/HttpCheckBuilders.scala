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
package io.gatling.http.check

import java.nio.CharBuffer

import io.gatling.core.check.{ Check, CheckFactory, Preparer }
import io.gatling.core.validation.SuccessWrapper
import io.gatling.http.check.HttpCheckOrder.{ Body, Checksum, Header, HttpCheckOrder, Status, Url }
import io.gatling.http.response.Response

object HttpCheckBuilders {

	private def httpCheckFactory(order: HttpCheckOrder): CheckFactory[HttpCheck, Response] = (wrapped: Check[Response]) => HttpCheck(wrapped, order)

	val statusCheckFactory = httpCheckFactory(Status)
	val urlCheckFactory = httpCheckFactory(Url)
	val checksumCheckFactory = httpCheckFactory(Checksum)
	val headerCheckFactory = httpCheckFactory(Header)
	val bodyCheckFactory = httpCheckFactory(Body)
	val timeCheckFactory = httpCheckFactory(Body)

	val passThroughResponsePreparer: Preparer[Response, Response] = (r: Response) => r.success
	val charsResponsePreparer: Preparer[Response, CharBuffer] = (response: Response) => response.charBuffer.success
	val stringResponsePreparer: Preparer[Response, String] = (response: Response) => response.getResponseBody.success
}
