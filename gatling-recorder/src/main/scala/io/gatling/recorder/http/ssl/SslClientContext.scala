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

package io.gatling.recorder.http.ssl

import javax.net.ssl.{ SSLContext, SSLEngine }

import io.gatling.recorder.http.flows.Remote

import io.netty.handler.ssl.util.InsecureTrustManagerFactory

private[http] object SslClientContext {

  val SslContext = {
    val clientContext = SSLContext.getInstance(SslServerContext.Protocol)
    clientContext.init(null, InsecureTrustManagerFactory.INSTANCE.getTrustManagers, null)
    clientContext
  }

  def createSSLEngine(remote: Remote): SSLEngine = {
    val engine = SslContext.createSSLEngine(remote.host, remote.port)
    engine.setUseClientMode(true)
    // FIXME have an option for disabling, eg hostname verification
    val params = engine.getSSLParameters
    params.setEndpointIdentificationAlgorithm("HTTPS")
    engine.setSSLParameters(params)
    engine
  }
}
