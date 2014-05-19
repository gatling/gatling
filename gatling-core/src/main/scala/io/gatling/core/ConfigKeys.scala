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
    val DisableCompiler = "gatling.core.disableCompiler"
    val Mute = "gatling.core.mute"

    object extract {
      object regex {
        val Cache = "gatling.core.extract.regex.cache"
      }
      object xpath {
        val Cache = "gatling.core.extract.xpath.cache"
      }
      object jsonPath {
        val Cache = "gatling.core.extract.jsonPath.cache"

        object jackson {
          val AllowComments = "gatling.core.extract.jsonPath.jackson.allowComments"
          val AllowUnquotedFieldNames = "gatling.core.extract.jsonPath.jackson.allowUnquotedFieldNames"
          val AllowSingleQuotes = "gatling.core.extract.jsonPath.jackson.allowSingleQuotes"
        }
      }
      object css {
        val Cache = "gatling.core.extract.css.cache"
      }
    }
    object timeOut {
      val Simulation = "gatling.core.timeOut.simulation"
    }
    object directory {
      val Data = "gatling.core.directory.data"
      val RequestBodies = "gatling.core.directory.requestBodies"
      val Simulations = "gatling.core.directory.simulations"
      val Binaries = "gatling.core.directory.binaries"
      val ReportsOnly = "gatling.core.directory.reportsOnly"
      val Results = "gatling.core.directory.results"
    }
    object zinc {
      val JvmArgs = "gatling.core.zinc.jvmArgs"
    }
  }

  object charting {
    val NoReports = "gatling.charting.noReports"
    val StatsTsvSeparator = "gatling.charting.statsTsvSeparator"
    val MaxPlotPerSeries = "gatling.charting.maxPlotPerSeries"
    val Accuracy = "gatling.charting.accuracy"

    object indicators {
      val LowerBound = "gatling.charting.indicators.lowerBound"
      val HigherBound = "gatling.charting.indicators.higherBound"
      val Percentile1 = "gatling.charting.indicators.percentile1"
      val Percentile2 = "gatling.charting.indicators.percentile2"
    }
  }

  object http {
    val CacheELFileBodies = "gatling.http.cacheELFileBodies"
    val CacheRawFileBodies = "gatling.http.cacheRawFileBodies"
    val WarmUpUrl = "gatling.http.warmUpUrl"

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
      val AllowPoolingConnection = "gatling.http.ahc.allowPoolingConnection"
      val AllowSslConnectionPool = "gatling.http.ahc.allowSslConnectionPool"
      val CompressionEnabled = "gatling.http.ahc.compressionEnabled"
      val ConnectionTimeOut = "gatling.http.ahc.connectionTimeout"
      val IdleConnectionInPoolTimeoutInMs = "gatling.http.ahc.idleConnectionInPoolTimeoutInMs"
      val IdleConnectionTimeoutInMs = "gatling.http.ahc.idleConnectionTimeoutInMs"
      val MaxConnectionLifeTimeInMs = "gatling.http.ahc.maxConnectionLifeTimeInMs"
      val IoThreadMultiplier = "gatling.http.ahc.ioThreadMultiplier"
      val MaximumConnectionsPerHost = "gatling.http.ahc.maximumConnectionsPerHost"
      val MaximumConnectionsTotal = "gatling.http.ahc.maximumConnectionsTotal"
      val MaxRetry = "gatling.http.ahc.maxRetry"
      val RequestTimeoutInMs = "gatling.http.ahc.requestTimeoutInMs"
      val UseProxyProperties = "gatling.http.ahc.useProxyProperties"
      val UseRawUrl = "gatling.http.ahc.useRawUrl"
      val WebSocketIdleTimeoutInMs = "gatling.http.ahc.webSocketIdleTimeoutInMs"
      val UseRelativeURIsWithSSLProxies = "gatling.http.ahc.useRelativeURIsWithSSLProxies"
    }
  }

  object data {
    val Writers = "gatling.data.writers"
    val Reader = "gatling.data.reader"

    object file {
      val BufferSize = "gatling.data.file.bufferSize"
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
      val MaxMeasuredValue = "gatling.data.graphite.maxMeasuredValue"
      val BufferSize = "gatling.data.graphite.bufferSize"
    }
    object jdbc {
      val Url = "gatling.data.jdbc.db.url"
      val Username = "gatling.data.jdbc.db.username"
      val Password = "gatling.data.jdbc.db.password"
      val BufferSize = "gatling.data.jdbc.bufferSize"

      object create {
        val CreateRunRecordTable = "gatling.data.jdbc.create.createRunRecordTable"
        val CreateRequestRecordTable = "gatling.data.jdbc.create.createRequestRecordTable"
        val CreateScenarioRecordTable = "gatling.data.jdbc.create.createScenarioRecordTable"
        val CreateGroupRecordTable = "gatling.data.jdbc.create.createGroupRecordTable"
      }
      object insert {
        val InsertRunRecord = "gatling.data.jdbc.insert.insertRunRecord"
        val InsertRequestRecord = "gatling.data.jdbc.insert.insertRequestRecord"
        val InsertScenarioRecord = "gatling.data.jdbc.insert.insertScenarioRecord"
        val InsertGroupRecord = "gatling.data.jdbc.insert.insertGroupRecord"
      }
    }
  }

}
