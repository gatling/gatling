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

package io.gatling.recorder.ui

import scala.concurrent.duration._

import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.model._

private[recorder] sealed trait FrontEndEvent

private[recorder] final case class PauseFrontEndEvent(duration: FiniteDuration) extends FrontEndEvent {
  private val toPrint = if (duration > 1.second) s"${duration.toSeconds}s" else s"${duration.length}ms"
  override def toString = s"PAUSE $toPrint"
}

private[recorder] final case class RequestFrontEndEvent(request: HttpRequest, response: HttpResponse)(implicit configuration: RecorderConfiguration)
    extends FrontEndEvent {

  val requestBody = new String(request.body, configuration.core.encoding)

  val responseBody = new String(response.body, configuration.core.encoding)

  override def toString = s"${request.method} | ${request.uri}"
}

private[recorder] final case class SslFrontEndEvent(uri: String) extends FrontEndEvent

private[recorder] final case class TagFrontEndEvent(tag: String) extends FrontEndEvent {
  override def toString = s"TAG | $tag"
}
