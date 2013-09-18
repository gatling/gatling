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
package io.gatling.core.config

import scala.reflect.ClassTag

object ProtocolRegistry {

	def apply(protocols: Seq[Protocol]) = {
		val indexedProtocols: Map[Class[_ <: Protocol], Protocol] = protocols
			.groupBy(_.getClass)
			.map {
				case (protocolType, configs) =>
					if (configs.length > 1) throw new ExceptionInInitializerError(s"Protocol ${protocolType.getName} configured multiple times")
					(protocolType -> configs.head)
			}.toMap

		new ProtocolRegistry(indexedProtocols)
	}
}

/**
 * A placeholder for Protocols
 */
class ProtocolRegistry(protocols: Map[Class[_ <: Protocol], Protocol]) {

	/**
	 * @param protocolType
	 * @return a registered Protocol according to its type
	 */
	def getProtocol[T <: Protocol: ClassTag]: Option[T] = protocols.get(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]).map(_.asInstanceOf[T])

	def getProtocol[T <: Protocol: ClassTag](default: => T): T = getProtocol[T].getOrElse(default)
}
