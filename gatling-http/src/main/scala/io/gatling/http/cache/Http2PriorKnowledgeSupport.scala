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

package io.gatling.http.cache

import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.client.Http2PriorKnowledge
import io.gatling.http.protocol.{ HttpProtocol, Remote }
import io.gatling.http.response.Response

import com.typesafe.scalalogging.StrictLogging

private[http] object Http2PriorKnowledgeSupport extends StrictLogging {
  private[cache] val Http2PriorKnowledgeAttributeName: String = SessionPrivateAttributes.generatePrivateAttribute("http.cache.priorKnowledgeHttp2")

  def setHttp2PriorKnowledge(httpProtocol: HttpProtocol): Session => Session =
    if (httpProtocol.enginePart.enableHttp2) {
      // seeded with the remotes configured in the protocol, completed with the protocol negotiated with the other ones
      _.set(Http2PriorKnowledgeAttributeName, httpProtocol.enginePart.http2PriorKnowledge)
    } else {
      Session.Identity
    }

  /**
   * The prior knowledge cache, or the one configured in the protocol if the HTTP cache was flushed, as flushing resets what was learned from the responses, not
   * the configuration.
   */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def priorKnowledgeCache(session: Session, httpProtocol: HttpProtocol): Map[Remote, Http2PriorKnowledge] =
    session.attributes
      .get(Http2PriorKnowledgeAttributeName)
      .map(_.asInstanceOf[Map[Remote, Http2PriorKnowledge]])
      .getOrElse(httpProtocol.enginePart.http2PriorKnowledge)

  def updateSessionHttp2PriorKnowledge(session: Session, httpProtocol: HttpProtocol, response: Response): Session = {
    val priorKnowledgeMap = priorKnowledgeCache(session, httpProtocol)
    val remote = Remote(response.request.getUri)
    if (priorKnowledgeMap.contains(remote)) {
      session
    } else {
      val http2Support = if (response.isHttp2) Http2PriorKnowledge.HTTP2_SUPPORTED else Http2PriorKnowledge.HTTP1_ONLY
      session.set(Http2PriorKnowledgeAttributeName, priorKnowledgeMap + (remote -> http2Support))
    }
  }

  /**
   * @return
   *   None when the remote's support for HTTP/2 is unknown, or when HTTP/2 isn't enabled
   */
  def getHttp2PriorKnowledge(session: Session, httpProtocol: HttpProtocol, remote: Remote): Option[Http2PriorKnowledge] =
    if (httpProtocol.enginePart.enableHttp2) {
      priorKnowledgeCache(session, httpProtocol).get(remote)
    } else {
      None
    }
}
