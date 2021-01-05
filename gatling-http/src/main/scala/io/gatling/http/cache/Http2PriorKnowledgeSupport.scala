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

package io.gatling.http.cache

import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.protocol.{ HttpProtocol, Remote }
import io.gatling.http.response.Response

import com.typesafe.scalalogging.StrictLogging

private[http] object Http2PriorKnowledgeSupport extends StrictLogging {
  val Http2PriorKnowledgeAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.cache.priorKnowledgeHttp2"

  def setHttp2PriorKnowledge(httpProtocol: HttpProtocol): Session => Session =
    if (httpProtocol.enginePart.enableHttp2) {
      _.set(Http2PriorKnowledgeAttributeName, httpProtocol.enginePart.http2PriorKnowledge)
    } else {
      logger.debug("HTTP/2 disabled")
      if (httpProtocol.enginePart.http2PriorKnowledge.nonEmpty) {
        logger.debug("Ignoring configured HTTP/2 prior knowledge")
      }
      Session.Identity
    }

  def updateSessionHttp2PriorKnowledge(session: Session, response: Response): Session = {
    val remote = Remote(response.request.getUri)
    session(Http2PriorKnowledgeSupport.Http2PriorKnowledgeAttributeName).asOption[Map[Remote, Boolean]] match {
      case Some(priorKnowledgeMap) =>
        if (priorKnowledgeMap.contains(remote)) {
          session
        } else {
          session.set(Http2PriorKnowledgeAttributeName, priorKnowledgeMap + (remote -> response.isHttp2))
        }
      case _ => session
    }
  }

  def isHttp2PriorKnowledge(session: Session, remote: Remote): Option[Boolean] =
    session(Http2PriorKnowledgeSupport.Http2PriorKnowledgeAttributeName).asOption[Map[Remote, Boolean]] match {
      case Some(priorKnowledgeMap) => priorKnowledgeMap.get(remote)
      case _                       => None
    }
}
