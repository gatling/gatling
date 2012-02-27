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
package com.excilys.ebi.gatling.core.resource

import scala.collection.mutable
import grizzled.slf4j.Logging

/**
 * The ResourceRegistry is responsible for storing a callback function that closes a resource
 * for each resource that has to be closed.
 *
 * It is the responsibility of the developer not to forgive to register its resources.
 */
object ResourceRegistry extends Logging {

	// not thread-safe
	private val onCloseCallbacks: mutable.HashSet[() => Any] = mutable.HashSet.empty

	/**
	 * Registers the resource
	 */
	def registerOnCloseCallback(onCloseCallback: () => Any) = {
		onCloseCallbacks += onCloseCallback
	}

	/**
	 * This method tries to close all the resources properly
	 * If one fails the others are still closed and a warning is sent
	 * to the logs
	 */
	def closeAll = {
		for (onCloseCallback <- onCloseCallbacks) {
			try {
				onCloseCallback()
			} catch {
				case e => error("Could not close resource", e)
			}
		}
	}
}
