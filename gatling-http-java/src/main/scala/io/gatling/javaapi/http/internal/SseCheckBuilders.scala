/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.http.internal

import io.gatling.javaapi.core.CheckBuilder

object SseCheckBuilders {
  val sseEvent: CheckBuilder.Find[String] =
    new CheckBuilder.Find.Default(io.gatling.http.Predef.sseEvent, SseCheckType.SseEvent, classOf[String], null)

  val sseData: CheckBuilder.Find[String] =
    new CheckBuilder.Find.Default(io.gatling.http.Predef.sseData, SseCheckType.SseData, classOf[String], null)

  val sseId: CheckBuilder.Find[String] =
    new CheckBuilder.Find.Default(io.gatling.http.Predef.sseId, SseCheckType.SseId, classOf[String], null)

  val sseRetry: CheckBuilder.Find[Integer] =
    new CheckBuilder.Find.Default(io.gatling.http.Predef.sseRetry, SseCheckType.SseRetry, classOf[Integer], (int: Int) => int.asInstanceOf[Integer])
}
