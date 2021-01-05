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

package io.gatling.http.protocol

import io.gatling.core.protocol.ProtocolComponents
import io.gatling.core.session.Session
import io.gatling.http.cache._
import io.gatling.http.engine.HttpEngine
import io.gatling.http.engine.tx.HttpTxExecutor

final class HttpComponents(
    val httpProtocol: HttpProtocol,
    val httpEngine: HttpEngine,
    val httpCaches: HttpCaches,
    val httpTxExecutor: HttpTxExecutor
) extends ProtocolComponents {

  override lazy val onStart: Session => Session =
    (SslContextSupport.setSslContexts(httpProtocol, httpEngine)
      andThen httpCaches.setNameResolver(httpProtocol.dnsPart, httpEngine)
      andThen LocalAddressSupport.setLocalAddresses(httpProtocol)
      andThen BaseUrlSupport.setHttpBaseUrl(httpProtocol)
      andThen BaseUrlSupport.setWsBaseUrl(httpProtocol)
      andThen Http2PriorKnowledgeSupport.setHttp2PriorKnowledge(httpProtocol))

  override lazy val onExit: Session => Unit =
    session => {
      httpCaches.nameResolver(session).foreach(_.close())
      SslContextSupport.sslContexts(session).foreach(_.close())
      httpEngine.flushClientIdChannels(session.userId, session.eventLoop)
    }
}
