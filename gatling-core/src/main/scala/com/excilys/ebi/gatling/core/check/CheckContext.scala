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

import com.excilys.ebi.gatling.core.log.Logging

object CheckContext extends Logging {

	private[check] val contextHolder = new ThreadLocal[mutable.Map[String, Any]]

	def useCheckContext(block: => Unit) {
		try {
			block
		} finally {
			contextHolder.set(null)
		}
	}

	private def getContext = {
		Option(contextHolder.get).getOrElse {
			val context = new mutable.HashMap[String, Any]()
			contextHolder.set(context)
			context
		}
	}

	def getCheckContextAttribute[T](key: String): Option[T] = getContext.get(key).asInstanceOf[Option[T]]

	def setAndReturnCheckContextAttribute[T](key: String, value: T): T = {
		getContext.put(key, value)
		value
	}
}