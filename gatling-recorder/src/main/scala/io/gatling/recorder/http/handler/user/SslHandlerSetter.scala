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
package io.gatling.recorder.http.handler.user

import io.gatling.recorder.http.channel.BootstrapFactory._
import io.gatling.recorder.http.ssl.SslServerContext

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel._
import io.netty.handler.ssl.SslHandler

/**
 * Placed on the server side pipeline, it replaces itself with a SslHandler when it sees the 200 response to the CONNECT request
 * (as CONNECT happens over HTTP, not HTTPS)
 */
private[handler] class SslHandlerSetter(domainAlias: String, sslServerContext: SslServerContext) extends ChannelOutboundHandlerAdapter with StrictLogging {

  override def write(ctx: ChannelHandlerContext, msg: AnyRef, promise: ChannelPromise): Unit = {
    ctx.pipeline
      .addAfter(SslHandlerSetterName, SslHandlerName, new SslHandler(sslServerContext.createSSLEngine(domainAlias)))
      .remove(SslHandlerSetterName)
    super.write(ctx, msg, promise)
  }
}
