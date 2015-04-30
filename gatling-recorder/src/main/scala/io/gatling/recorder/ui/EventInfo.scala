/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import io.gatling.recorder.config.RecorderConfiguration
import java.nio.charset.Charset
import org.jboss.netty.handler.codec.http.{ HttpMessage, HttpRequest, HttpResponse }
import scala.concurrent.duration.{ DurationLong, FiniteDuration }

private[recorder] sealed trait EventInfo

private[recorder] case class PauseInfo(duration: FiniteDuration) extends EventInfo {
  val toPrint = if (duration > 1.second) s"${duration.toSeconds}s" else s"${duration.length}ms"
  override def toString = s"PAUSE $toPrint"
}

private[recorder] case class RequestInfo(request: HttpRequest, response: HttpResponse)(implicit configuration: RecorderConfiguration) extends EventInfo {

  private def getHttpBody(message: HttpMessage) = message.getContent.toString(Charset.forName(configuration.core.encoding))

  val requestBody = getHttpBody(request)

  val responseBody = getHttpBody(response)

  override def toString = s"${request.getMethod} | ${request.getUri}"
}

private[recorder] case class SSLInfo(uri: String) extends EventInfo

private[recorder] case class TagInfo(tag: String) extends EventInfo {
  override def toString = s"TAG | $tag"
}
