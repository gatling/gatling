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
package io.gatling.charts.result.reader.buffers

import scala.collection.mutable

import io.gatling.charts.result.reader.RequestRecord
import io.gatling.core.result.Group
import io.gatling.core.result.message.Status

trait TransactionsPerSecBuffers {

	val transactionsPerSecBuffers = mutable.Map.empty[BufferKey, CountBuffer]

	def getTransactionsPerSecBuffer(requestName: Option[String], group: Option[Group], status: Option[Status]): CountBuffer =
		transactionsPerSecBuffers.getOrElseUpdate(BufferKey(requestName, group, status), new CountBuffer)

	def updateTransactionsPerSecBuffers(record: RequestRecord) {
		getTransactionsPerSecBuffer(Some(record.name), record.group, None).update(record.responseEndBucket)
		getTransactionsPerSecBuffer(Some(record.name), record.group, Some(record.status)).update(record.responseEndBucket)

		getTransactionsPerSecBuffer(None, None, None).update(record.responseEndBucket)
		getTransactionsPerSecBuffer(None, None, Some(record.status)).update(record.responseEndBucket)
	}
}