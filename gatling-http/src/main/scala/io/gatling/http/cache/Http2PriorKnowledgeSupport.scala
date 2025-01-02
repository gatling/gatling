/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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
      _.set(Http2PriorKnowledgeAttributeName, Map.empty)
    } else {
      Session.Identity
    }

  def updateSessionHttp2PriorKnowledge(session: Session, response: Response): Session = {
    val priorKnowledgeMap = session.attributes(Http2PriorKnowledgeSupport.Http2PriorKnowledgeAttributeName).asInstanceOf[Map[Remote, Http2PriorKnowledge]]
    val remote = Remote(response.request.getUri)
    if (priorKnowledgeMap.contains(remote)) {
      session
    } else {
      val http2Support = if (response.isHttp2) Http2PriorKnowledge.HTTP2_SUPPORTED else Http2PriorKnowledge.HTTP1_ONLY
      session.set(Http2PriorKnowledgeAttributeName, priorKnowledgeMap + (remote -> http2Support))
    }
  }

  def getHttp2PriorKnowledge(session: Session, remote: Remote): Option[Http2PriorKnowledge] = {
    val priorKnowledgeMap = session.attributes(Http2PriorKnowledgeSupport.Http2PriorKnowledgeAttributeName).asInstanceOf[Map[Remote, Http2PriorKnowledge]]
    priorKnowledgeMap.get(remote)
  }
}
