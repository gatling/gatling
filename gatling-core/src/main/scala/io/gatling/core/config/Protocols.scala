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

import io.gatling.core.controller.throttle.ThrottlingProfile
import io.gatling.core.pause._
import io.gatling.core.session.Session

import scala.reflect.ClassTag

object Protocols {

  def apply(protocols: Protocol*) = new Protocols(Map.empty, Constant, None, None) ++ protocols
}

/**
 * A placeholder for Protocols
 */
case class Protocols(protocols: Map[Class[_ <: Protocol], Protocol], pauseType: PauseType, globalThrottling: Option[ThrottlingProfile], scenarioThrottling: Option[ThrottlingProfile]) {

  /**
   * @return a registered Protocol according to its type
   */
  def protocol[T <: Protocol: ClassTag]: Option[T] = protocols.get(implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]).map(_.asInstanceOf[T])

  def +(protocol: Protocol): Protocols = copy(protocols = protocols + (protocol.getClass -> protocol))
  def ++(protocols: Iterable[Protocol]): Protocols = copy(protocols = this.protocols ++ protocols.map(p => p.getClass -> p))

  def ++(other: Protocols) = copy(protocols = protocols ++ other.protocols)

  def warmUp(implicit configuration: GatlingConfiguration): Unit =
    protocols.values.foreach(_.warmUp)

  val userEnd: Session => Unit =
    session => protocols.values.foreach(_.userEnd(session))
}
