/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.decoupled.models

import java.time.Instant

import scala.util.Try

import io.circe.Decoder

sealed trait ExecutionPhase {
  def name: String
  def time: Instant
}

object ExecutionPhase {
  val triggerPhaseName = "Trigger"

  def apply(name: String, time: Instant): ExecutionPhase = new NormalExecutionPhase(name, time)
}

final case class TriggerPhase(time: Instant) extends ExecutionPhase {
  val name = ExecutionPhase.triggerPhaseName
}

final case class NormalExecutionPhase(name: String, time: Instant) extends ExecutionPhase

final case class MissingPhase(name: String, time: Instant) extends ExecutionPhase

trait ExecutionPhaseCirceFormat {
  implicit val decodeInstant: Decoder[Instant] = Decoder.decodeLong.emapTry { millis =>
    Try(Instant.ofEpochMilli(millis))
  }
}
