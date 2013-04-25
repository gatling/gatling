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
package io.gatling.core.result.message

import io.gatling.core.util.FileHelper.tabulationSeparator

sealed abstract class MessageType {
	def name: String
	def recordLength: Int
	def unapply(string: String) = {
		val array = string.split(tabulationSeparator)
		if (array.length >= recordLength && array(0) == name) Some(array) else None
	}
}
object RunMessageType extends MessageType {
	val name = "RUN"
	val recordLength = 4
}
object RequestMessageType extends MessageType {
	val name = "REQUEST"
	val recordLength = 10
}
object ScenarioMessageType extends MessageType {
	val name = "SCENARIO"
	val recordLength = 5
}
object GroupMessageType extends MessageType {
	val name = "GROUP"
	val recordLength = 7
}