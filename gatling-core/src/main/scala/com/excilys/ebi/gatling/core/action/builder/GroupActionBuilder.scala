/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.action.builder
import com.excilys.ebi.gatling.core.action.Action

/**
 * Companion of GroupActionBuilder class
 */
object GroupActionBuilder {
	/**
	 * Defines the beginning of a group
	 *
	 * Warning, isEnd is set to true for the beginning because of the chain of builders. Indeed,
	 * it is unfolded in reverse order
	 *
	 * @param groupName the name of the group
	 * @return a GroupActionBuilder that indicates the beginning of the group named groupName.
	 */
	def startGroupBuilder(groupName: String) = new GroupActionBuilder(groupName, true)

	/**
	 * Defines the end of a group
	 *
	 * Warning, isEnd is set to false for the end because of the chain of builders. Indeed,
	 * it is unfolded in reverse order
	 *
	 * @param groupName the name of the group
	 * @return a GroupActionBuilder that indicates the end of the group named groupName.
	 */
	def endGroupBuilder(groupName: String) = new GroupActionBuilder(groupName, false)
}

/**
 * This class is used to group requests
 *
 * @constructor creates a group action builder
 * @param name the name of the group to create
 * @param end if this builder defines the start or the end of the group
 */
class GroupActionBuilder(val name: String, val head: Boolean) extends AbstractActionBuilder {

	/**
	 * This method should never be called
	 *
	 * @throws UnsupportedOperationException
	 */
	def build = throw new UnsupportedOperationException

	/**
	 * This method should never be called
	 *
	 * @throws UnsupportedOperationException
	 */
	def withNext(next: Action) = throw new UnsupportedOperationException

	/**
	 * This method should never be called
	 *
	 * @throws UnsupportedOperationException
	 */
	def inGroups(groups: List[String]) = throw new UnsupportedOperationException
}
