/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.core.stats.message

object MessageEvent {
  def apply(name: String): MessageEvent = name match {
    case Start.name => Start
    case End.name   => End
    case _          => throw new IllegalArgumentException(s"Illegal MessageEvent value $name")
  }

  case object Start extends MessageEvent("START")
  case object End extends MessageEvent("END")
}

sealed abstract class MessageEvent(val name: String)
