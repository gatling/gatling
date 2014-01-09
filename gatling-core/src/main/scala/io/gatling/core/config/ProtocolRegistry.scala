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

/**
 * A placeholder for Protocols
 */
case class ProtocolRegistry(protocols: Map[Class[_ <: Protocol], Protocol] = Map.empty) {

	/**
	 * @param protocolType
	 * @return a registered Protocol according to its type
	 */
	def getProtocol[T <: Protocol: ClassTag]: Option[T] = protocols.get(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]).map(_.asInstanceOf[T])

	def register(protocol: Protocol): ProtocolRegistry = register(Seq(protocol))
	def register(protocols: Seq[Protocol]): ProtocolRegistry = ProtocolRegistry(this.protocols ++ protocols.map(p => p.getClass -> p))

	def ++(other: ProtocolRegistry) = ProtocolRegistry(protocols ++ other.protocols)

	def warmUp {
		protocols.foreach { case (_, protocol) => protocol.warmUp }
	}
}
