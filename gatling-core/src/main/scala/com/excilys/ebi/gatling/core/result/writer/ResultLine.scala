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
package com.excilys.ebi.gatling.core.result.writer

import java.io.{ Writer, StringWriter }

import com.excilys.ebi.gatling.core.result.message.ResultStatus.ResultStatus
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR

object ResultLine {

	object Headers {
		val RUN_ON = "RUN_ON"
		val SCENARIO_NAME = "SCENARIO_NAME"
		val USER_ID = "USER_ID"
		val REQUEST_NAME = "REQUEST_NAME"
		val EXECUTION_START_DATE = "EXECUTION_START_DATE"
		val EXECUTION_END_DATE = "EXECUTION_END_DATE"
		val REQUEST_SENDING_END_DATE = "REQUEST_SENDING_END_DATE"
		val RESPONSE_RECEIVING_START_DATE = "RESPONSE_RECEIVING_START_DATE"
		val RESULT_STATUS = "RESULT_STATUS"
		val RESULT_MESSAGE = "RESULT_MESSAGE"

		def print(writer: Writer) = writer
			.append(RUN_ON).append(TABULATION_SEPARATOR)
			.append(SCENARIO_NAME).append(TABULATION_SEPARATOR)
			.append(USER_ID).append(TABULATION_SEPARATOR)
			.append(REQUEST_NAME).append(TABULATION_SEPARATOR)
			.append(EXECUTION_START_DATE).append(TABULATION_SEPARATOR)
			.append(EXECUTION_END_DATE).append(TABULATION_SEPARATOR)
			.append(REQUEST_SENDING_END_DATE).append(TABULATION_SEPARATOR)
			.append(RESPONSE_RECEIVING_START_DATE).append(TABULATION_SEPARATOR)
			.append(RESULT_STATUS).append(TABULATION_SEPARATOR)
			.append(RESULT_MESSAGE).append(TABULATION_SEPARATOR)

		def check(s: String) = {
			if (s != print(new StringWriter()).toString) throw new IllegalArgumentException("The string doesn't match the expected headers")
		}
	}
}

case class ResultLine(runOn: String, scenarioName: String, userId: Int, requestName: String, executionStartDate: Long, executionEndDate: Long, requestSendingEndDate: Long, responseReceivingStartDate: Long, resultStatus: ResultStatus, resultMessage: String) {

	lazy val responseTime = executionEndDate - executionStartDate

	lazy val latency = responseReceivingStartDate - requestSendingEndDate

	def print(writer: Writer) = writer
		.append(runOn).append(TABULATION_SEPARATOR)
		.append(scenarioName).append(TABULATION_SEPARATOR)
		.append(userId.toString).append(TABULATION_SEPARATOR)
		.append(requestName).append(TABULATION_SEPARATOR)
		.append(executionStartDate.toString).append(TABULATION_SEPARATOR)
		.append(executionEndDate.toString).append(TABULATION_SEPARATOR)
		.append(requestSendingEndDate.toString).append(TABULATION_SEPARATOR)
		.append(responseReceivingStartDate.toString).append(TABULATION_SEPARATOR)
		.append(resultStatus.toString).append(TABULATION_SEPARATOR)
		.append(resultMessage).append(TABULATION_SEPARATOR)
}