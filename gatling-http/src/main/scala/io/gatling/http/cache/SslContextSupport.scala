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

import javax.net.ssl.KeyManagerFactory

import scala.util.control.NonFatal

import io.gatling.commons.util.Throwables._
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.engine.HttpEngine
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.util.SslContexts

import com.typesafe.scalalogging.StrictLogging

private[http] object SslContextSupport extends StrictLogging {

  private val HttpSslContextsAttributeName: String = SessionPrivateAttributes.PrivateAttributePrefix + "http.ssl.sslContexts"

  private def resolvePerUserKeyManagerFactory(session: Session, perUserKeyManagerFactory: Option[Long => KeyManagerFactory]): Option[KeyManagerFactory] =
    perUserKeyManagerFactory match {
      case Some(kmf) =>
        try {
          Some(kmf(session.userId))
        } catch {
          case NonFatal(e) =>
            logger.error(s"Can't build perUserKeyManagerFactory: ${e.rootMessage}", e)
            None
        }
      case _ => None
    }

  def setSslContexts(httpProtocol: HttpProtocol, httpEngine: HttpEngine): Session => Session =
    if (httpProtocol.enginePart.shareConnections) {
      Session.Identity
    } else { session =>
      {
        val kmf = resolvePerUserKeyManagerFactory(session, httpProtocol.enginePart.perUserKeyManagerFactory)
        session.set(HttpSslContextsAttributeName, httpEngine.newSslContexts(httpProtocol.enginePart.enableHttp2, kmf))
      }
    }

  def sslContexts(session: Session): Option[SslContexts] =
    session.attributes.get(HttpSslContextsAttributeName).map(_.asInstanceOf[SslContexts])
}
