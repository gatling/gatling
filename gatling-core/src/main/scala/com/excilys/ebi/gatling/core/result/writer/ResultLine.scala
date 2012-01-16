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

import org.joda.time.DateTime

import com.excilys.ebi.gatling.core.result.message.ResultStatus.ResultStatus
import com.excilys.ebi.gatling.core.util.DateHelper.printResultDate
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR

object ResultLine {

	object Headers {
		val RUN_ON = "RUN_ON"
		val SCENARIO_NAME = "SCENARIO_NAME"
		val USER_ID = "USER_ID"
		val REQUEST_NAME = "REQUEST_NAME"
		val EXECUTION_START_DATE = "EXECUTION_START_DATE"
		val EXECUTION_DURATION_IN_MILLIS = "EXECUTION_DURATION_IN_MILLIS"
		val RESULT_STATUS = "RESULT_STATUS"
		val RESULT_MESSAGE = "RESULT_MESSAGE"

		def print(writer: Writer) = writer
			.append(RUN_ON).append(TABULATION_SEPARATOR)
			.append(SCENARIO_NAME).append(TABULATION_SEPARATOR)
			.append(USER_ID).append(TABULATION_SEPARATOR)
			.append(REQUEST_NAME).append(TABULATION_SEPARATOR)
			.append(EXECUTION_START_DATE).append(TABULATION_SEPARATOR)
			.append(EXECUTION_DURATION_IN_MILLIS).append(TABULATION_SEPARATOR)
			.append(RESULT_STATUS).append(TABULATION_SEPARATOR)
			.append(RESULT_MESSAGE).append(TABULATION_SEPARATOR)

		def check(s: String) = {
			if (s != print(new StringWriter()).toString) throw new IllegalArgumentException("The string doesn't match the expected headers")
		}
	}
}

case class ResultLine(runOn: String, scenarioName: String, userId: Int, requestName: String, executionStartDate: DateTime, executionDurationInMillis: Long, resultStatus: ResultStatus, resultMessage: String) {

	def print(writer: Writer) = writer
		.append(runOn).append(TABULATION_SEPARATOR)
		.append(scenarioName).append(TABULATION_SEPARATOR)
		.append(userId.toString).append(TABULATION_SEPARATOR)
		.append(requestName).append(TABULATION_SEPARATOR)
		.append(printResultDate(executionStartDate)).append(TABULATION_SEPARATOR)
		.append(executionDurationInMillis.toString).append(TABULATION_SEPARATOR)
		.append(resultStatus.toString).append(TABULATION_SEPARATOR)
		.append(resultMessage).append(TABULATION_SEPARATOR)
}