/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
import io.gatling.http.ahc.HttpEngine
import io.gatling.http.cache.HttpCaches

import org.asynchttpclient.DefaultAsyncHttpClient
import org.asynchttpclient.netty.channel.pool.ChannelPoolPartitionSelector

case class HttpComponents(httpProtocol: HttpProtocol, httpEngine: HttpEngine, httpCaches: HttpCaches) extends ProtocolComponents {

  private val onExitF: Session => Unit = session => {
    val (_, ahc) = httpEngine.httpClient(session, httpProtocol)
    ahc.asInstanceOf[DefaultAsyncHttpClient].getChannelPool.flushPartitions(new ChannelPoolPartitionSelector() {

      val userId = session.userId

      override def select(partitionKey: Object): Boolean = partitionKey match {
        case (`userId`, _) => true
        case _             => false
      }
    })
  }

  def onExit: Option[Session => Unit] = Some(onExitF)
}
