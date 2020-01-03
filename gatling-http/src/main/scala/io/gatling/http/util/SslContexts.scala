/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

import java.io.Closeable
import java.security.SecureRandom

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

import io.gatling.core.config.HttpConfiguration

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.ssl._
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.util.ReferenceCountUtil
import javax.net.ssl._

private[http] object SslContextsFactory {
  private val DefaultSslSecureRandom = new SecureRandom
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
  private val useOpenSslFinalizers = httpConfig.advanced.useOpenSslFinalizers

  def newSslContexts(http2Enabled: Boolean, perUserKeyManagerFactory: Option[KeyManagerFactory]): SslContexts = {

    val kmf = perUserKeyManagerFactory.orElse(httpConfig.ssl.keyManagerFactory)
    val tmf = httpConfig.ssl.trustManagerFactory.orElse {
      if (httpConfig.advanced.useInsecureTrustManager) {
        Some(InsecureTrustManagerFactory.INSTANCE)
      } else {
        None
      }
    }

    if (useOpenSsl) {
      val provider = if (useOpenSslFinalizers) SslProvider.OPENSSL else SslProvider.OPENSSL_REFCNT
      val sslContextBuilder = SslContextBuilder.forClient.sslProvider(provider)

      if (httpConfig.advanced.sslSessionCacheSize > 0) {
        sslContextBuilder.sessionCacheSize(httpConfig.advanced.sslSessionCacheSize)
      }

      if (httpConfig.advanced.sslSessionTimeout > Duration.Zero) {
        sslContextBuilder.sessionTimeout(sslSessionTimeoutSeconds)
      }

      if (enabledProtocols.nonEmpty) {
        sslContextBuilder.protocols(enabledProtocols: _*)
      }

      if (httpConfig.advanced.sslEnabledCipherSuites.nonEmpty) {
        sslContextBuilder.ciphers(enabledCipherSuites)
      } else {
        sslContextBuilder.ciphers(null, IdentityCipherSuiteFilter.INSTANCE_DEFAULTING_TO_SUPPORTED_CIPHERS)
      }

      kmf.foreach(sslContextBuilder.keyManager)
      tmf.foreach(sslContextBuilder.trustManager)

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
      jdkSslContext.init(kmf.map(_.getKeyManagers).orNull, tmf.map(_.getTrustManagers).orNull, DefaultSslSecureRandom)

      val sslContext = newJdkSslContext(jdkSslContext, null)
      val alpnSslContext =
        if (http2Enabled) {
          Some(newJdkSslContext(jdkSslContext, Apn))
        } else {
          None
        }
      SslContexts(sslContext, alpnSslContext)
    }
  }

  private def newJdkSslContext(jdkSslContext: SSLContext, apn: ApplicationProtocolConfig): SslContext =
    new JdkSslContext(
      jdkSslContext,
      true,
      if (enabledCipherSuites.isEmpty) null else enabledCipherSuites,
      IdentityCipherSuiteFilter.INSTANCE_DEFAULTING_TO_SUPPORTED_CIPHERS,
      apn,
      ClientAuth.NONE,
      if (enabledProtocols.nonEmpty) enabledProtocols else null,
      false
    )
}

private[http] final case class SslContexts(sslContext: SslContext, alpnSslContext: Option[SslContext]) extends Closeable {
  override def close(): Unit = {
    ReferenceCountUtil.release(sslContext)
    alpnSslContext.foreach(ReferenceCountUtil.release)
  }
}
