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
package com.excilys.ebi.gatling.core.check
import scala.collection.mutable

/**
 * A context for performing check that might store resources so that they can be reused for multiple checks.
 * Resources are stored in a ThreadLocal.
 *
 */
object CheckContext {

	private val contextHolder = new ThreadLocal[mutable.Map[String, Any]]

	/**
	 * First, set a fresh context
	 * Then, execute the block of code
	 * Last, ensure context clean up
	 *
	 * @param block code to be executed in the context of performing checks
	 * @return the result of the block of code
	 */
	def useCheckContext[T](block: => T) = {
		try {
			contextHolder.set(new mutable.HashMap[String, Any])

			block
		} finally {
			contextHolder.set(null)
		}
	}

	/**
	 * @return the context in which to store resources
	 */
	private def context = Option(contextHolder.get).getOrElse(throw new UnsupportedOperationException("Context not set. You're probably trying to access the CheckContext outside of the useCheckContext scope"))

	/**
	 * @param key the key under which a resource might have been previously stored
	 * @param value a resource to be stored if none was previously stored
	 * @return the stored resource (old or new)
	 */
	def getOrUpdateCheckContextAttribute[T](key: String, value: => T): T = {
		context.get(key).asInstanceOf[Option[T]].getOrElse {
			val resolvedValue = value
			context.put(key, resolvedValue)
			resolvedValue
		}
	}
}