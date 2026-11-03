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

package io.gatling.http.engine

import io.gatling.commons.util.Throwables._
import io.gatling.core.actor.ActorRef
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.Controller
import io.gatling.http.client.{ HttpClient, HttpClientConfig }
import io.gatling.http.client.impl.DefaultHttpClient
import io.gatling.http.util._

import com.typesafe.scalalogging.StrictLogging

private[gatling] final class HttpClientFactory(
    sslContextsFactory: SslContextsFactory,
    configuration: GatlingConfiguration,
    controller: ActorRef[Controller.Command]
) extends StrictLogging {
  private val httpConfig = configuration.http
  private val socketConfig = configuration.socket
  private val sslConfig = configuration.ssl
  private val nettyConfig = configuration.netty

  private[gatling] def newClientConfig(): HttpClientConfig = {
    val defaultSslContexts = sslContextsFactory.newSslContexts(http2Enabled = true, None)
    new HttpClientConfig()
      .setDefaultSslContextsHolder(defaultSslContexts)
      .setDefaultCharset(configuration.core.charset)
      .setEnableHostnameVerification(httpConfig.enableHostnameVerification)
      .setChannelPoolIdleTimeout(httpConfig.pooledConnectionIdleTimeout.toMillis)
      .setConnectTimeout(socketConfig.connectTimeout.toMillis)
      .setSoKeepAlive(socketConfig.soKeepAlive)
      .setHandshakeTimeout(sslConfig.handshakeTimeout.toMillis)
      .setEnableSni(sslConfig.enableSni)
      .setUseNativeTransport(nettyConfig.useNativeTransport)
      .setUseIoUring(nettyConfig.useIoUring)
      .setFatalErrorHandler(crashLoadGenerator)
  }

  // gracefully stop the run as crashed instead of exiting the JVM, as Gatling might be embedded
  private def crashLoadGenerator(e: Throwable): Unit = {
    logger.error(s"HTTP client crashed with a fatal error, stopping: ${e.detailedMessage}", e)
    controller ! Controller.Command.StopLoadGenerator(Controller.Command.StopLoadGenerator.Reason.Crash.Unexpected(e))
  }

  def newClient: HttpClient = new DefaultHttpClient(newClientConfig())
}
