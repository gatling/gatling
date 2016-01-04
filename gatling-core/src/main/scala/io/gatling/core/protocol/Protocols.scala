/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.protocol

import scala.reflect.ClassTag

object Protocols {

  def apply(protocols: Protocol*): Protocols = apply(protocols.toIterable)
  def apply(protocols: Iterable[Protocol]): Protocols = new Protocols(Map.empty) ++ protocols
}

/**
 * A placeholder for Protocols
 */
case class Protocols(protocols: Map[Class[_ <: Protocol], Protocol]) {

  /**
   * @return a registered Protocol according to its type
   */
  def protocol[T <: Protocol: ClassTag]: Option[T] = {
    val protocolClass = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
    protocols.get(protocolClass).map(_.asInstanceOf[T])
  }

  def ++(protocols: Iterable[Protocol]): Protocols = copy(protocols = this.protocols ++ protocols.map(p => p.getClass.asInstanceOf[Class[Protocol]] -> p))

  def ++(other: Protocols) = copy(protocols = protocols ++ other.protocols)
}
