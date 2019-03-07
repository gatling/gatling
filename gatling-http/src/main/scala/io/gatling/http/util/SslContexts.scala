/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.http.util

import java.security.{ KeyStore, SecureRandom }

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

import io.gatling.core.config.HttpConfiguration

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.ssl._
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import javax.net.ssl._

private[http] object SslContextsFactory {
  private val DefaultSslSecureRandom = new SecureRandom
  private val DefaultTrustManagers: Array[TrustManager] = {
    val defaultTrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    defaultTrustManagerFactory.init(null.asInstanceOf[KeyStore])
    defaultTrustManagerFactory.getTrustManagers
  }
  private val Apn = new ApplicationProtocolConfig(
    ApplicationProtocolConfig.Protocol.ALPN,
    // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
    // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
    ApplicationProtocolNames.HTTP_2,
    ApplicationProtocolNames.HTTP_1_1
  )
}

private[gatling] class SslContextsFactory(httpConfig: HttpConfiguration) extends StrictLogging {

  import SslContextsFactory._

  private val sslSessionTimeoutSeconds = httpConfig.advanced.sslSessionTimeout.toSeconds
  private val enabledProtocols: Array[String] = httpConfig.advanced.sslEnabledProtocols.toArray
  private val enabledCipherSuites = httpConfig.advanced.sslEnabledCipherSuites.asJava
  private val useOpenSsl =
    if (httpConfig.advanced.useOpenSsl) {
      val available = OpenSsl.isAvailable
      if (!available) {
        logger.error("OpenSSL is enabled in the Gatling configuration but it's not available on your architecture.")
      }
      available
    } else {
      false
    }

  def newSslContexts(http2Enabled: Boolean, perUserKeyManagerFactory: Option[KeyManagerFactory]): SslContexts = {

    val kmf = perUserKeyManagerFactory.orElse(httpConfig.ssl.keyManagerFactory)

    if (useOpenSsl) {
      val sslContextBuilder = SslContextBuilder.forClient.sslProvider(SslProvider.OPENSSL)

      if (httpConfig.advanced.sslSessionCacheSize > 0) {
        sslContextBuilder.sessionCacheSize(httpConfig.advanced.sslSessionCacheSize)
      }

      if (httpConfig.advanced.sslSessionTimeout > Duration.Zero) {
        sslContextBuilder.sessionTimeout(sslSessionTimeoutSeconds)
      }

      if (enabledProtocols.length > 0) {
        sslContextBuilder.protocols(enabledProtocols: _*)
      }

      if (httpConfig.advanced.sslEnabledCipherSuites.nonEmpty) {
        sslContextBuilder.ciphers(enabledCipherSuites)
      } else {
        sslContextBuilder.ciphers(null, IdentityCipherSuiteFilter.INSTANCE_DEFAULTING_TO_SUPPORTED_CIPHERS)
      }

      kmf.foreach(sslContextBuilder.keyManager)

      httpConfig.ssl.trustManagerFactory match {
        case Some(tmf) => sslContextBuilder.trustManager(tmf)
        case _ =>
          if (httpConfig.advanced.useInsecureTrustManager) {
            sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE)
          }
      }

      val sslContext = sslContextBuilder.build
      val alpnSslContext =
        if (http2Enabled) {
          Some(sslContextBuilder.applicationProtocolConfig(Apn).build)
        } else {
          None
        }
      SslContexts(sslContext, alpnSslContext)

    } else {
      val jdkSslContext = SSLContext.getInstance("TLS")
      jdkSslContext.init(kmf.map(_.getKeyManagers).orNull, DefaultTrustManagers, DefaultSslSecureRandom)

      val sslContext = newSslContext(jdkSslContext, null)
      val alpnSslContext =
        if (http2Enabled) {
          Some(newSslContext(jdkSslContext, Apn))
        } else {
          None
        }
      SslContexts(sslContext, alpnSslContext)
    }
  }

  private def newSslContext(jdkSslContext: SSLContext, apn: ApplicationProtocolConfig): SslContext =
    new DelegatingSslContext(new JdkSslContext(
      jdkSslContext,
      true,
      if (enabledCipherSuites.isEmpty) null else enabledCipherSuites,
      IdentityCipherSuiteFilter.INSTANCE_DEFAULTING_TO_SUPPORTED_CIPHERS,
      apn,
      ClientAuth.NONE,
      null,
      false
    )) {
      override def initEngine(engine: SSLEngine): Unit =
        if (enabledProtocols.nonEmpty) {
          engine.setEnabledProtocols(enabledProtocols)
        }
    }
}

private[http] case class SslContexts(sslContext: SslContext, alplnSslContext: Option[SslContext])
