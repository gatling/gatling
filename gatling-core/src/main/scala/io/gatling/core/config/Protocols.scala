/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

object Protocols {

	def apply(protocols: Protocol*) = new Protocols() ++ protocols
}

/**
 * A placeholder for Protocols
 */
class Protocols(val protocols: Map[Class[_ <: Protocol], Protocol] = Map.empty) {

	/**
	 * @param protocolType
	 * @return a registered Protocol according to its type
	 */
	def getProtocol[T <: Protocol: ClassTag]: Option[T] = protocols.get(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]).map(_.asInstanceOf[T])

	def +(protocol: Protocol): Protocols = new Protocols(protocols + (protocol.getClass -> protocol))
	def ++(protocols: Seq[Protocol]): Protocols = new Protocols(this.protocols ++ protocols.map(p => p.getClass -> p))

	def ++(other: Protocols) = new Protocols(protocols ++ other.protocols)

	def warmUp() {
		protocols.foreach { case (_, protocol) => protocol.warmUp }
	}
}
