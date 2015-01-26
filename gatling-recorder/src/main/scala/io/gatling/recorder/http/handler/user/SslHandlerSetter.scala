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
package io.gatling.recorder.http.handler.user

import com.typesafe.scalalogging.StrictLogging
import io.gatling.recorder.http.channel.BootstrapFactory._
import io.gatling.recorder.http.ssl.SSLServerContext
import org.jboss.netty.channel._
import org.jboss.netty.handler.ssl.SslHandler

/**
 * Placed on the server side pipeline, it replaces itself with a SslHandler when it sees the 200 response to the CONNECT request
 * (as CONNECT happens over HTTP, not HTTPS)
 */
class SSLHandlerSetter(domainAlias: String, sslServerContext: SSLServerContext) extends ChannelDownstreamHandler with StrictLogging {

  override def handleDownstream(ctx: ChannelHandlerContext, e: ChannelEvent): Unit = {
    val sslHandler = new SslHandler(sslServerContext.createSSLEngine(domainAlias))
    sslHandler.setCloseOnSSLException(true)
    ctx.getPipeline.replace(SslHandlerName, SslHandlerName, sslHandler)
    ctx.sendDownstream(e)
  }
}
