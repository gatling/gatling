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

package io.gatling.core.config

object ConfigKeys {
  object core {
    val Encoding = "gatling.core.encoding"
    val ElFileBodiesCacheMaxCapacity = "gatling.core.elFileBodiesCacheMaxCapacity"
    val RawFileBodiesCacheMaxCapacity = "gatling.core.rawFileBodiesCacheMaxCapacity"
    val RawFileBodiesInMemoryMaxSize = "gatling.core.rawFileBodiesInMemoryMaxSize"
    val PebbleFileBodiesCacheMaxCapacity = "gatling.core.pebbleFileBodiesCacheMaxCapacity"
    val FeederAdaptiveLoadModeThreshold = "gatling.core.feederAdaptiveLoadModeThreshold"
    val ShutdownTimeout = "gatling.core.shutdownTimeout"

    object extract {
      object regex {
        val CacheMaxCapacity = "gatling.core.extract.regex.cacheMaxCapacity"
      }
      object xpath {
        val CacheMaxCapacity = "gatling.core.extract.xpath.cacheMaxCapacity"
      }
      object jsonPath {
        val CacheMaxCapacity = "gatling.core.extract.jsonPath.cacheMaxCapacity"
      }
      object css {
        val CacheMaxCapacity = "gatling.core.extract.css.cacheMaxCapacity"
      }
    }
  }

  object socket {
    val ConnectTimeout = "gatling.socket.connectTimeout"
    val TcpNoDelay = "gatling.socket.tcpNoDelay"
    val SoKeepAlive = "gatling.socket.soKeepAlive"
  }

  object netty {
    val UseNativeTransport = "gatling.netty.useNativeTransport"
    val UseIoUring = "gatling.netty.useIoUring"
    val Allocator = "gatling.netty.allocator"
    val MaxThreadLocalCharBufferSize = "gatling.netty.maxThreadLocalCharBufferSize"
  }

  object ssl {
    val UseOpenSsl = "gatling.ssl.useOpenSsl"
    val UseOpenSslFinalizers = "gatling.ssl.useOpenSslFinalizers"
    val HandshakeTimeout = "gatling.ssl.handshakeTimeout"
    val UseInsecureTrustManager = "gatling.ssl.useInsecureTrustManager"
    val EnabledProtocols = "gatling.ssl.enabledProtocols"
    val EnabledCipherSuites = "gatling.ssl.enabledCipherSuites"
    val SessionCacheSize = "gatling.ssl.sessionCacheSize"
    val SessionTimeout = "gatling.ssl.sessionTimeout"
    val EnableSni = "gatling.ssl.enableSni"

    object keyStore {
      val Type = "gatling.ssl.keyStore.type"
      val File = "gatling.ssl.keyStore.file"
      val Password = "gatling.ssl.keyStore.password"
      val Algorithm = "gatling.ssl.keyStore.algorithm"
    }

    object trustStore {
      val Type = "gatling.ssl.trustStore.type"
      val File = "gatling.ssl.trustStore.file"
      val Password = "gatling.ssl.trustStore.password"
      val Algorithm = "gatling.ssl.trustStore.algorithm"
    }
  }

  object charting {
    val MaxPlotPerSeries = "gatling.charting.maxPlotPerSeries"
    val UseGroupDurationMetric = "gatling.charting.useGroupDurationMetric"

    object indicators {
      val LowerBound = "gatling.charting.indicators.lowerBound"
      val HigherBound = "gatling.charting.indicators.higherBound"
      val Percentile1 = "gatling.charting.indicators.percentile1"
      val Percentile2 = "gatling.charting.indicators.percentile2"
      val Percentile3 = "gatling.charting.indicators.percentile3"
      val Percentile4 = "gatling.charting.indicators.percentile4"
    }
  }

  object http {
    val FetchedCssCacheMaxCapacity = "gatling.http.fetchedCssCacheMaxCapacity"
    val FetchedHtmlCacheMaxCapacity = "gatling.http.fetchedHtmlCacheMaxCapacity"
    val PerUserCacheMaxCapacity = "gatling.http.perUserCacheMaxCapacity"
    val WarmUpUrl = "gatling.http.warmUpUrl"
    val PooledConnectionIdleTimeout = "gatling.http.pooledConnectionIdleTimeout"
    val RequestTimeout = "gatling.http.requestTimeout"
    val EnableHostnameVerification = "gatling.http.enableHostnameVerification"

    object dns {
      val QueryTimeout = "gatling.http.dns.queryTimeout"
      val MaxQueriesPerResolve = "gatling.http.dns.maxQueriesPerResolve"
    }
  }

  object jms {
    val ReplyTimeoutScanPeriod = "gatling.jms.replyTimeoutScanPeriod"
  }

  object data {
    val UtcDateTime = "gatling.data.utcDateTime"
    val Writers = "gatling.data.writers"

    object console {
      val Light = "gatling.data.console.light"
      val WritePeriod = "gatling.data.console.writePeriod"
    }
    val EnableAnalytics = "gatling.data.enableAnalytics"
  }
}
