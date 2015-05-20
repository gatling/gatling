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
package io.gatling.http.ahc

import io.gatling.core.session.Session
import io.gatling.http.request.HttpRequest
import io.gatling.http.response._

import akka.actor.ActorRef

object HttpTx {

  def silent(request: HttpRequest, root: Boolean): Boolean = {

      def silentBecauseProtocolSilentResources = !root && request.config.protocol.requestPart.silentResources

      def silentBecauseProtocolSilentURI: Option[Boolean] = request.config.protocol.requestPart.silentURI
        .map(_.matcher(request.ahcRequest.getUrl).matches)

    request.config.silent.orElse(silentBecauseProtocolSilentURI).getOrElse(silentBecauseProtocolSilentResources)
  }
}

case class HttpTx(session: Session,
                  request: HttpRequest,
                  responseBuilderFactory: ResponseBuilderFactory,
                  next: ActorRef,
                  root: Boolean = true,
                  redirectCount: Int = 0,
                  update: Session => Session = Session.Identity) {

  val silent: Boolean = HttpTx.silent(request, root)
}
