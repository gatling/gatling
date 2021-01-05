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

package io.gatling.http.util

import java.{ util => ju }
import java.security.SecureRandom
import javax.net.ssl._

import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._

import io.gatling.core.config.SslConfiguration

import com.typesafe.scalalogging.StrictLogging
import io.netty.handler.ssl._
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.util.ReferenceCountUtil

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

private[gatling] class SslContextsFactory(sslConfig: SslConfiguration) extends StrictLogging {

  import SslContextsFactory._

  private val useOpenSsl =
    if (sslConfig.useOpenSsl) {
      val available = OpenSsl.isAvailable
      if (!available) {
        logger.error("OpenSSL is enabled in the Gatling configuration but it's not available on your architecture.")
      }
      available
    } else {
      false
    }
  private val sslSessionTimeoutSeconds = sslConfig.sessionTimeout.toSeconds
  private lazy val DefaultJavaSslParameters = {
    val context = SSLContext.getInstance("TLS")
    context.init(null, null, null);
    context.getDefaultSSLParameters
  }
  private val enabledProtocols: Array[String] =
    if (useOpenSsl) {
      sslConfig.enabledProtocols.toArray
    } else {
      val supportedProtocols = DefaultJavaSslParameters.getProtocols.toSet
      sslConfig.enabledProtocols.toArray.filter(supportedProtocols.contains)
    }
  private val enabledCipherSuites: ju.List[String] = {
    if (useOpenSsl) {
      sslConfig.enabledCipherSuites.asJava
    } else {
      val supportedCipherSuites = DefaultJavaSslParameters.getCipherSuites
      sslConfig.enabledCipherSuites.filter(supportedCipherSuites.contains).asJava
    }
  }
  private val useOpenSslFinalizers = sslConfig.useOpenSslFinalizers

  def newSslContexts(http2Enabled: Boolean, perUserKeyManagerFactory: Option[KeyManagerFactory]): SslContexts = {

    val kmf = perUserKeyManagerFactory.orElse(sslConfig.keyManagerFactory)
    val tmf = sslConfig.trustManagerFactory.orElse {
      if (sslConfig.useInsecureTrustManager) {
        Some(InsecureTrustManagerFactory.INSTANCE)
      } else {
        None
      }
    }

    if (useOpenSsl) {
      val provider = if (useOpenSslFinalizers) SslProvider.OPENSSL else SslProvider.OPENSSL_REFCNT
      val sslContextBuilder = SslContextBuilder.forClient.sslProvider(provider)

      if (sslConfig.sessionCacheSize > 0) {
        sslContextBuilder.sessionCacheSize(sslConfig.sessionCacheSize)
      }

      if (sslConfig.sessionTimeout > Duration.Zero) {
        sslContextBuilder.sessionTimeout(sslSessionTimeoutSeconds)
      }

      if (enabledProtocols.nonEmpty) {
        sslContextBuilder.protocols(enabledProtocols: _*)
      }

      if (sslConfig.enabledCipherSuites.nonEmpty) {
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
      new SslContexts(sslContext, alpnSslContext)

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
      new SslContexts(sslContext, alpnSslContext)
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

private[http] final class SslContexts(val sslContext: SslContext, val alpnSslContext: Option[SslContext]) extends AutoCloseable {
  override def close(): Unit = {
    ReferenceCountUtil.release(sslContext)
    alpnSslContext.foreach(ReferenceCountUtil.release)
  }
}
