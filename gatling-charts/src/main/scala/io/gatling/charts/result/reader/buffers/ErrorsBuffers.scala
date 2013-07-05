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
package io.gatling.charts.result.reader.buffers

import scala.collection.mutable

import io.gatling.charts.result.reader.RequestRecord
import io.gatling.core.result.Group

trait ErrorsBuffers {

	val errorsBuffers = mutable.Map.empty[BufferKey, mutable.Map[String, Int]]

	def getErrorsBuffers(requestName: Option[String], group: Option[Group]) =
		errorsBuffers.getOrElseUpdate(BufferKey(requestName, group, None), mutable.Map.empty[String, Int])

	def updateErrorBuffers(record: RequestRecord) {

		def updateGlobalError(errorMessage: String) {
			val buffer = getErrorsBuffers(None, None)
			buffer += errorMessage -> (buffer.getOrElseUpdate(errorMessage, 0) + 1)
		}

		def updateGroupError(errorMessage: String) {
			record.group.foreach { group =>
				val buffer = getErrorsBuffers(None, Some(group))
				buffer += errorMessage -> (buffer.getOrElseUpdate(errorMessage, 0) + 1)
			}
		}

		def updateRequestError(errorMessage: String) {
			val buffer = getErrorsBuffers(Some(record.name), record.group)
			buffer += errorMessage -> (buffer.getOrElseUpdate(errorMessage, 0) + 1)
		}

		record.errorMessage.foreach { errorMessage =>
			updateGlobalError(errorMessage)
			updateGroupError(errorMessage)
			updateRequestError(errorMessage)
		}
	}
}
