/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.util.Ssl._
import io.gatling.commons.util.SystemProps._
import io.gatling.core.CoreComponents
import io.gatling.http.client.{ HttpClient, HttpClientConfig }
import io.gatling.http.client.impl.DefaultHttpClient

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

private[gatling] object HttpClientFactory {

  def apply(coreComponents: CoreComponents): HttpClientFactory =
    coreComponents.configuration.resolve(
      // [fl]
      //
      //
      //
      //
      //
      // [fl]
      new DefaultHttpClientFactory(coreComponents)
    )
}

private[gatling] trait HttpClientFactory {

  def newClient: HttpClient
}

private[gatling] class DefaultHttpClientFactory(coreComponents: CoreComponents)
  extends HttpClientFactory
  with EventLoopGroups
  with StrictLogging {

  private val configuration = coreComponents.configuration
  private val ahcConfig = configuration.http.ahc
  setSystemPropertyIfUndefined("io.netty.allocator.type", configuration.http.ahc.allocator)
  setSystemPropertyIfUndefined("io.netty.maxThreadLocalCharBufferSize", configuration.http.ahc.maxThreadLocalCharBufferSize)

  private[gatling] def newClientConfig(): HttpClientConfig = {
    val clientConfig = new HttpClientConfig()
      .setConnectTimeout(ahcConfig.connectTimeout)
      .setHandshakeTimeout(ahcConfig.handshakeTimeout)
      .setChannelPoolIdleTimeout(ahcConfig.pooledConnectionIdleTimeout)
      .setMaxRetry(ahcConfig.maxRetry)
      .setDisableHttpsEndpointIdentificationAlgorithm(ahcConfig.disableHttpsEndpointIdentificationAlgorithm)
      .setEnabledSslProtocols(ahcConfig.sslEnabledProtocols match {
        case Nil => null
        case ps  => ps.toArray
      })
      .setFilterInsecureCipherSuites(ahcConfig.filterInsecureCipherSuites)
      .setWebSocketMaxFramePayloadLength(Int.MaxValue)
      .setDefaultCharset(configuration.core.charset)
      .setUseOpenSsl(ahcConfig.useOpenSsl)
      .setUseNativeTransport(ahcConfig.useNativeTransport)
      .setSslSessionCacheSize(ahcConfig.sslSessionCacheSize)
      .setSslSessionTimeout(ahcConfig.sslSessionTimeout)
      .setDisableSslSessionResumption(ahcConfig.disableSslSessionResumption)
      .setTcpNoDelay(ahcConfig.tcpNoDelay)
      .setSoReuseAddress(ahcConfig.soReuseAddress)
      .setEnableZeroCopy(ahcConfig.enableZeroCopy)
      .setThreadPoolName("gatling-http")

    if (ahcConfig.sslEnabledCipherSuites.nonEmpty) {
      clientConfig.setEnabledSslCipherSuites(ahcConfig.sslEnabledCipherSuites.toArray)
    }

    val keyManagerFactory = configuration.http.ssl.keyStore
      .map(config => newKeyManagerFactory(config.storeType, config.file, config.password, config.algorithm))
      .orNull

    val trustManagerFactory = configuration.http.ssl.trustStore
      .map(config => newTrustManagerFactory(config.storeType, config.file, config.password, config.algorithm))
      .orElse(if (ahcConfig.useInsecureTrustManager) Some(InsecureTrustManagerFactory.INSTANCE) else None).orNull

    clientConfig
      .setKeyManagerFactory(keyManagerFactory)
      .setTrustManagerFactory(trustManagerFactory)
  }

  override def newClient: HttpClient = {
    val client = new DefaultHttpClient(newClientConfig())
    coreComponents.actorSystem.registerOnTermination(client.close())
    client
  }
}
