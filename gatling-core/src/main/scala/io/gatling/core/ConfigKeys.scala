/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.core

object ConfigKeys {

  object core {
    val OutputDirectoryBaseName = "gatling.core.outputDirectoryBaseName"
    val RunDescription = "gatling.core.runDescription"
    val Encoding = "gatling.core.encoding"
    val SimulationClass = "gatling.core.simulationClass"
    val Mute = "gatling.core.mute"
    val ElFileBodiesCacheMaxCapacity = "gatling.core.elFileBodiesCacheMaxCapacity"
    val RawFileBodiesCacheMaxCapacity = "gatling.core.rawFileBodiesCacheMaxCapacity"
    val RawFileBodiesInMemoryMaxSize = "gatling.core.rawFileBodiesInMemoryMaxSize"

    object extract {
      object regex {
        val CacheMaxCapacity = "gatling.core.extract.regex.cacheMaxCapacity"
      }
      object xpath {
        val CacheMaxCapacity = "gatling.core.extract.xpath.cacheMaxCapacity"
      }
      object jsonPath {
        val CacheMaxCapacity = "gatling.core.extract.jsonPath.cacheMaxCapacity"
        val PreferJackson = "gatling.core.extract.jsonPath.preferJackson"
      }
      object css {
        val CacheMaxCapacity = "gatling.core.extract.css.cacheMaxCapacity"
      }
    }
    object directory {
      val Data = "gatling.core.directory.data"
      val Bodies = "gatling.core.directory.bodies"
      val Simulations = "gatling.core.directory.simulations"
      val Binaries = "gatling.core.directory.binaries"
      val ReportsOnly = "gatling.core.directory.reportsOnly"
      val Results = "gatling.core.directory.results"
    }
  }

  object charting {
    val NoReports = "gatling.charting.noReports"
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
    val EnableGA = "gatling.http.enableGA"

    object ssl {
      object keyStore {
        val Type = "gatling.http.ssl.keyStore.type"
        val File = "gatling.http.ssl.keyStore.file"
        val Password = "gatling.http.ssl.keyStore.password"
        val Algorithm = "gatling.http.ssl.keyStore.algorithm"
      }
      object trustStore {
        val Type = "gatling.http.ssl.trustStore.type"
        val File = "gatling.http.ssl.trustStore.file"
        val Password = "gatling.http.ssl.trustStore.password"
        val Algorithm = "gatling.http.ssl.trustStore.algorithm"
      }
    }

    object ahc {
      val KeepAlive = "gatling.http.ahc.keepAlive"
      val ConnectTimeout = "gatling.http.ahc.connectTimeout"
      val HandshakeTimeout = "gatling.http.ahc.handshakeTimeout"
      val PooledConnectionIdleTimeout = "gatling.http.ahc.pooledConnectionIdleTimeout"
      val ReadTimeout = "gatling.http.ahc.readTimeout"
      val MaxRetry = "gatling.http.ahc.maxRetry"
      val RequestTimeout = "gatling.http.ahc.requestTimeout"
      val AcceptAnyCertificate = "gatling.http.ahc.acceptAnyCertificate"
      val HttpClientCodecMaxInitialLineLength = "gatling.http.ahc.httpClientCodecMaxInitialLineLength"
      val HttpClientCodecMaxHeaderSize = "gatling.http.ahc.httpClientCodecMaxHeaderSize"
      val HttpClientCodecMaxChunkSize = "gatling.http.ahc.httpClientCodecMaxChunkSize"
      val WebSocketMaxFrameSize = "gatling.http.ahc.webSocketMaxFrameSize"
      val SslEnabledProtocols = "gatling.http.ahc.sslEnabledProtocols"
      val SslEnabledCipherSuites = "gatling.http.ahc.sslEnabledCipherSuites"
      val SslSessionCacheSize = "gatling.http.ahc.sslSessionCacheSize"
      val SslSessionTimeout = "gatling.http.ahc.sslSessionTimeout"
      val UseOpenSsl = "gatling.http.ahc.useOpenSsl"
      val UseNativeTransport = "gatling.http.ahc.useNativeTransport"
      val UsePooledMemory = "gatling.http.ahc.usePooledMemory"
      val TcpNoDelay = "gatling.http.ahc.tcpNoDelay"
      val SoReuseAddress = "gatling.http.ahc.soReuseAddress"
      val SoLinger = "gatling.http.ahc.soLinger"
      val SoSndBuf = "gatling.http.ahc.soSndBuf"
      val SoRcvBuf = "gatling.http.ahc.soRcvBuf"
      val Allocator = "gatling.http.ahc.allocator"
      val MaxThreadLocalCharBufferSize = "gatling.http.ahc.maxThreadLocalCharBufferSize"
    }

    object dns {
      val QueryTimeout = "gatling.http.dns.queryTimeout"
      val MaxQueriesPerResolve = "gatling.http.dns.maxQueriesPerResolve"
    }
  }

  object jms {
    val AcknowledgedMessagesBufferSize = "gatling.jms.acknowledgedMessagesBufferSize"
  }

  object data {
    val Writers = "gatling.data.writers"

    object file {
      val BufferSize = "gatling.data.file.bufferSize"
    }
    object leak {
      val NoActivityTimeout = "gatling.data.leak.noActivityTimeout"
    }
    object console {
      val Light = "gatling.data.console.light"
    }
    object graphite {
      val Light = "gatling.data.graphite.light"
      val Host = "gatling.data.graphite.host"
      val Port = "gatling.data.graphite.port"
      val Protocol = "gatling.data.graphite.protocol"
      val RootPathPrefix = "gatling.data.graphite.rootPathPrefix"
      val BufferSize = "gatling.data.graphite.bufferSize"
      val WriteInterval = "gatling.data.graphite.writeInterval"
    }
  }

  // [fl]
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  // [fl]
}
