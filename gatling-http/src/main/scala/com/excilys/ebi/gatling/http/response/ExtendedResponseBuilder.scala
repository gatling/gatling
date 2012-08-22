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
package com.excilys.ebi.gatling.http.response

import java.lang.System.{ currentTimeMillis, nanoTime }
import java.security.MessageDigest

import scala.math.max

import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.excilys.ebi.gatling.http.check.bodypart.ChecksumCheck
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.http.request.HttpPhase.CompletePageReceived
import com.ning.http.client.{ HttpResponseBodyPart, HttpResponseHeaders, HttpResponseStatus, Request }
import com.ning.http.client.Response.ResponseBuilder

object ExtendedResponseBuilder {

	def newExtendedResponseBuilder(checks: List[HttpCheck[_]], protocolConfiguration: HttpProtocolConfiguration): ExtendedResponseBuilderFactory = {

		val checksumChecks = checks.foldLeft(List.empty[ChecksumCheck]) { (checksumChecks, check) =>
			check match {
				case checksumCheck: ChecksumCheck => checksumCheck :: checksumChecks
				case _ => checksumChecks
			}
		}

		val storeBodyPart = !protocolConfiguration.responseChunksDiscardingEnabled || checks.exists(_.phase == CompletePageReceived)
		(request: Request, session: Session) => new ExtendedResponseBuilder(request, session, checksumChecks, storeBodyPart)
	}
}

class ExtendedResponseBuilder(request: Request, session: Session, checksumChecks: List[ChecksumCheck], storeBodyParts: Boolean) {

	private val responseBuilder = new ResponseBuilder
	private var checksums = Map.empty[String, MessageDigest]
	private var executionStartDateNanos = nanoTime
	var _executionStartDate = currentTimeMillis
	var _requestSendingEndDate = 0L
	var _responseReceivingStartDate = 0L
	var _executionEndDate = 0L

	private def computeTimeFromNanos(nanos: Long) = (nanos - executionStartDateNanos) / 1000000 + _executionStartDate

	def accumulate(responseStatus: HttpResponseStatus) {
		responseBuilder.accumulate(responseStatus)
	}

	def accumulate(headers: HttpResponseHeaders) {
		responseBuilder.accumulate(headers)
	}

	def updateRequestSendingEndDate(nanos: Long) {
		_requestSendingEndDate = computeTimeFromNanos(nanos)
	}

	def updateResponseReceivingStartDate(nanos: Long) {
		_responseReceivingStartDate = computeTimeFromNanos(nanos)
	}

	def computeExecutionEndDateFromNanos(nanos: Long) {
		_executionEndDate = computeTimeFromNanos(nanos)
	}

	def accumulate(bodyPart: Option[HttpResponseBodyPart]) {
		bodyPart.map { part =>
			for (check <- checksumChecks) {
				val algorithm = check.algorithm
				checksums.getOrElse(algorithm, {
					val md = MessageDigest.getInstance(algorithm)
					checksums += (algorithm -> md)
					md
				}).update(part.getBodyPartBytes)
			}

			if (storeBodyParts)
				responseBuilder.accumulate(part)
		}
	}

	def build: ExtendedResponse = {
		_requestSendingEndDate = max(_requestSendingEndDate, _executionStartDate)
		_responseReceivingStartDate = max(_responseReceivingStartDate, _requestSendingEndDate)
		_executionEndDate = max(_executionEndDate, _executionEndDate)
		val response = Option(responseBuilder.build)
		new ExtendedResponse(request, response, checksums, _executionStartDate, _requestSendingEndDate, _responseReceivingStartDate, _executionEndDate)
	}
}