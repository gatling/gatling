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
package com.excilys.ebi.gatling.core.result.message

/**
 * This case class is to be sent to the logging actor, it contains all the information
 * required for statistics generation after the simulation has run
 *
 * @param scenarioName the name of the current scenario
 * @param userId the id of the current user being simulated
 * @param action the name of the action that was made
 * @param executionStartDate the date on which the action was made
 * @param executionDuration the duration of the action
 * @param resultStatus the status of the action
 * @param resultMessage the message of the action on completion
 */
case class ActionInfo(scenarioName: String, userId: Int, action: String, executionStartDate: Long, executionDuration: Long, endOfRequestSendingDate: Long, startOfResponseReceivingDate: Long, resultStatus: ResultStatus.ResultStatus,
	resultMessage: String)