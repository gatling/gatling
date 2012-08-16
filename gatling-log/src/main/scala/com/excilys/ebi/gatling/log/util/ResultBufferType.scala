/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.log.util

import com.excilys.ebi.gatling.core.result.message.RequestStatus

object ResultBufferType extends Enumeration {
	type ResultBufferType = Value
	val GLOBAL, BY_STATUS, BY_REQUEST, BY_STATUS_AND_REQUEST, BY_SCENARIO = Value

	def getResultBufferType(status: Option[RequestStatus.RequestStatus], requestName: Option[String]) = {
		(status, requestName) match {
			case (Some(_), Some(_)) => BY_STATUS_AND_REQUEST
			case (None, Some(_)) => BY_REQUEST
			case (Some(_), None) => BY_STATUS
			case (None, None) => GLOBAL
		}
	}
}
