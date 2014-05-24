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
package io.gatling.recorder.config

import io.gatling.core.util.ClassSimpleNameToString

sealed trait FilterStrategy extends ClassSimpleNameToString { def name: String }

object FilterStrategy {

  case object WhitelistFirst extends FilterStrategy { val name = "Whitelist First" }
  case object BlacklistFirst extends FilterStrategy { val name = "Blacklist First" }
  case object Disabled extends FilterStrategy { val name = "Disabled" }

  val AllStrategies = List(WhitelistFirst, BlacklistFirst, Disabled)

  def fromString(s: String): FilterStrategy =
    AllStrategies.find(_.toString == s).getOrElse {
      throw new IllegalArgumentException(s"$s is not a valid filter strategy")
    }
}
