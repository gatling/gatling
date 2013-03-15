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
package com.excilys.ebi.gatling.core.config

import scala.reflect.ClassTag

object ProtocolConfigurationRegistry {

	def apply(configurations: Seq[ProtocolConfiguration]): ProtocolConfigurationRegistry = {
		val indexedConfigurations: Map[Class[_ <: ProtocolConfiguration], ProtocolConfiguration] = configurations
			.groupBy(_.getClass)
			.map {
				case (protocolType, configs) =>
					if (configs.length > 1) throw new ExceptionInInitializerError(s"Multiple configurations defined for propocol ${protocolType.getName}")
					(protocolType -> configs.head)
			}.toMap

		new ProtocolConfigurationRegistry(indexedConfigurations)
	}
}

/**
 * A placeholder for ProtocolConfigurations
 */
class ProtocolConfigurationRegistry(configurations: Map[Class[_ <: ProtocolConfiguration], ProtocolConfiguration]) {

	/**
	 * @param protocolType
	 * @return a registered ProtocolConfiguration according to its type
	 */
	def getProtocolConfiguration[T <: ProtocolConfiguration: ClassTag]: Option[T] = configurations.get(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]).map(_.asInstanceOf[T])

	def getProtocolConfiguration[T <: ProtocolConfiguration: ClassTag](default: => T): T = getProtocolConfiguration[T].getOrElse(default)
}