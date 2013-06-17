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
package io.gatling.http.response

import java.security.MessageDigest
import java.util.ArrayList

import scala.collection.mutable
import scala.math.max

import com.ning.http.client.{ HttpResponseBodyPart, HttpResponseHeaders, HttpResponseStatus, Request }

import io.gatling.core.util.StringHelper.bytes2Hex
import io.gatling.core.util.TimeHelper.{ computeTimeMillisFromNanos, nowMillis }
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.HttpCheckOrder.Body
import io.gatling.http.check.checksum.ChecksumCheck
import io.gatling.http.config.HttpProtocol

object ResponseBuilder {

	def newResponseBuilder(checks: List[HttpCheck], responseProcessor: Option[ResponseProcessor], protocol: HttpProtocol): ResponseBuilderFactory = {

		val checksumChecks = checks.collect {
			case checksumCheck: ChecksumCheck => checksumCheck
		}

		val storeBodyParts = !protocol.discardResponseChunks || checks.exists(_.order == Body)
		request: Request => new ResponseBuilder(request, checksumChecks, responseProcessor, storeBodyParts)
	}
}

class ResponseBuilder(request: Request, checksumChecks: List[ChecksumCheck], responseProcessor: Option[ResponseProcessor], storeBodyParts: Boolean) {

	val firstByteSent = nowMillis
	var lastByteSent = 0L
	var firstByteReceived = 0L
	var lastByteReceived = 0L
	private var status: HttpResponseStatus = _
	private var headers: HttpResponseHeaders = _
	private val bodies = new ArrayList[HttpResponseBodyPart]
	private var digests = mutable.Map.empty[String, MessageDigest]

	def accumulate(status: HttpResponseStatus) = {
		this.status = status
		this
	}

	def accumulate(headers: HttpResponseHeaders) = {
		this.headers = headers
		this
	}

	def updateLastByteSent(nanos: Long) = {
		lastByteSent = computeTimeMillisFromNanos(nanos)
		this
	}

	def updateFirstByteReceived(nanos: Long) = {
		firstByteReceived = computeTimeMillisFromNanos(nanos)
		this
	}

	def updateLastByteReceived(nanos: Long) = {
		lastByteReceived = computeTimeMillisFromNanos(nanos)
		this
	}

	def accumulate(bodyPart: Option[HttpResponseBodyPart]) = {
		bodyPart.map { part =>
			for (check <- checksumChecks) {
				val algorithm = check.algorithm
				digests.getOrElseUpdate(algorithm, MessageDigest.getInstance(algorithm)).update(part.getBodyByteBuffer)
			}

			if (storeBodyParts) bodies.add(part)
		}
		this
	}

	def build: Response = {
		// time measurement is imprecise due to multi-core nature
		// ensure request doesn't end before starting
		lastByteSent = max(lastByteSent, firstByteSent)
		// ensure response doesn't start before request ends
		firstByteReceived = max(firstByteReceived, lastByteSent)
		// ensure response doesn't end before starting
		lastByteReceived = max(lastByteReceived, firstByteReceived)
		val ahcResponse = Option(status).map(_.provider.prepareResponse(status, headers, bodies))
		val checksums = digests.mapValues(md => bytes2Hex(md.digest)).toMap
		val rawResponse = HttpResponse(request, ahcResponse, checksums, firstByteSent, lastByteSent, firstByteReceived, lastByteReceived)

		responseProcessor
			.map(_.applyOrElse(rawResponse, identity[Response]))
			.getOrElse(rawResponse)
	}
}