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
package com.excilys.ebi.gatling.http.response

import java.security.MessageDigest

import scala.math.max

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.util.TimeHelper.{ computeTimeMillisFromNanos, nowMillis }
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.check.bodypart.ChecksumCheck
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived
import com.ning.http.client.{ HttpResponseBodyPart, HttpResponseHeaders, HttpResponseStatus, Request }

object ExtendedResponseBuilder {

	def newExtendedResponseBuilder(checks: List[HttpCheck], protocolConfiguration: HttpProtocolConfiguration): ExtendedResponseBuilderFactory = {

		val checksumChecks = checks.foldLeft(List.empty[ChecksumCheck]) { (checksumChecks, check) =>
			check match {
				case checksumCheck: ChecksumCheck => checksumCheck :: checksumChecks
				case _ => checksumChecks
			}
		}

		val storeBodyParts = !protocolConfiguration.responseChunksDiscardingEnabled || checks.exists(_.phase == CompletePageReceived)
		(request: Request, session: Session) => new ExtendedResponseBuilder(request, session, checksumChecks, storeBodyParts)
	}
}

class ExtendedResponseBuilder(request: Request, session: Session, checksumChecks: List[ChecksumCheck], storeBodyParts: Boolean) {

	private var status: HttpResponseStatus = _
	private var headers: HttpResponseHeaders = _
	private val bodies = new java.util.ArrayList[HttpResponseBodyPart]
	private var checksums = Map.empty[String, MessageDigest]
	val _executionStartDate = nowMillis
	var _requestSendingEndDate = 0L
	var _responseReceivingStartDate = 0L
	var _executionEndDate = 0L

	def accumulate(status: HttpResponseStatus) {
		this.status = status
	}

	def accumulate(headers: HttpResponseHeaders) {
		this.headers = headers
	}

	def updateRequestSendingEndDate(nanos: Long) {
		_requestSendingEndDate = computeTimeMillisFromNanos(nanos)
	}

	def updateResponseReceivingStartDate(nanos: Long) {
		_responseReceivingStartDate = computeTimeMillisFromNanos(nanos)
	}

	def computeExecutionEndDateFromNanos(nanos: Long) {
		_executionEndDate = computeTimeMillisFromNanos(nanos)
	}

	def accumulate(bodyPart: Option[HttpResponseBodyPart]) {
		bodyPart.map { part =>
			for (check <- checksumChecks) {
				val algorithm = check.algorithm
				checksums.getOrElse(algorithm, {
					val md = MessageDigest.getInstance(algorithm)
					checksums += (algorithm -> md)
					md
				}).update(part.getBodyByteBuffer)
			}

			if (storeBodyParts) {
				bodies.add(part)
			}
		}
	}

	def build: ExtendedResponse = {
		// time measurement is imprecise due to multi-core nature
		// ensure request doesn't end before starting
		_requestSendingEndDate = max(_requestSendingEndDate, _executionStartDate)
		// ensure response doesn't start before request ends
		_responseReceivingStartDate = max(_responseReceivingStartDate, _requestSendingEndDate)
		// ensure response doesn't end before starting
		_executionEndDate = max(_executionEndDate, _responseReceivingStartDate)
		val response = Option(status).map(status => status.provider.prepareResponse(status, headers, bodies))
		new ExtendedResponse(request, response, checksums, _executionStartDate, _requestSendingEndDate, _responseReceivingStartDate, _executionEndDate)
	}
}