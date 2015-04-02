/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
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
    val RedirectPerUserCacheMaxCapacity = "gatling.http.redirectPerUserCacheMaxCapacity"
    val ExpirePerUserCacheMaxCapacity = "gatling.http.expirePerUserCacheMaxCapacity"
    val LastModifiedPerUserCacheMaxCapacity = "gatling.http.lastModifiedPerUserCacheMaxCapacity"
    val EtagPerUserCacheMaxCapacity = "gatling.http.etagPerUserCacheMaxCapacity"
    val WarmUpUrl = "gatling.http.warmUpUrl"
    val EnableGA = "gatling.http.enableGA"

    object ssl {
      object trustStore {
        val Type = "gatling.http.ssl.trustStore.type"
        val File = "gatling.http.ssl.trustStore.file"
        val Password = "gatling.http.ssl.trustStore.password"
        val Algorithm = "gatling.http.ssl.trustStore.algorithm"
      }
      object keyStore {
        val Type = "gatling.http.ssl.keyStore.type"
        val File = "gatling.http.ssl.keyStore.file"
        val Password = "gatling.http.ssl.keyStore.password"
        val Algorithm = "gatling.http.ssl.keyStore.algorithm"
      }
    }

    object ahc {
      val AllowPoolingConnections = "gatling.http.ahc.allowPoolingConnections"
      val AllowPoolingSslConnections = "gatling.http.ahc.allowPoolingSslConnections"
      val CompressionEnforced = "gatling.http.ahc.compressionEnforced"
      val ConnectTimeout = "gatling.http.ahc.connectTimeout"
      val PooledConnectionIdleTimeout = "gatling.http.ahc.pooledConnectionIdleTimeout"
      val ReadTimeout = "gatling.http.ahc.readTimeout"
      val ConnectionTTL = "gatling.http.ahc.connectionTTL"
      val IoThreadMultiplier = "gatling.http.ahc.ioThreadMultiplier"
      val MaxConnectionsPerHost = "gatling.http.ahc.maxConnectionsPerHost"
      val MaxConnections = "gatling.http.ahc.maxConnections"
      val MaxRetry = "gatling.http.ahc.maxRetry"
      val RequestTimeout = "gatling.http.ahc.requestTimeout"
      val UseProxyProperties = "gatling.http.ahc.useProxyProperties"
      val WebSocketTimeout = "gatling.http.ahc.webSocketTimeout"
      val UseRelativeURIsWithConnectProxies = "gatling.http.ahc.useRelativeURIsWithConnectProxies"
      val AcceptAnyCertificate = "gatling.http.ahc.acceptAnyCertificate"
      val HttpClientCodecMaxInitialLineLength = "gatling.http.ahc.httpClientCodecMaxInitialLineLength"
      val HttpClientCodecMaxHeaderSize = "gatling.http.ahc.httpClientCodecMaxHeaderSize"
      val HttpClientCodecMaxChunkSize = "gatling.http.ahc.httpClientCodecMaxChunkSize"
      val KeepEncodingHeader = "gatling.http.ahc.keepEncodingHeader"
      val WebSocketMaxFrameSize = "gatling.http.ahc.webSocketMaxFrameSize"
      val SslEnabledProtocols = "gatling.http.ahc.sslEnabledProtocols"
      val SslEnabledCipherSuites = "gatling.http.ahc.sslEnabledCipherSuites"
      val SslSessionCacheSize = "gatling.http.ahc.sslSessionCacheSize"
      val SslSessionTimeout = "gatling.http.ahc.sslSessionTimeout"
    }
  }

  object data {
    val Writers = "gatling.data.writers"
    val Reader = "gatling.data.reader"

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
}
