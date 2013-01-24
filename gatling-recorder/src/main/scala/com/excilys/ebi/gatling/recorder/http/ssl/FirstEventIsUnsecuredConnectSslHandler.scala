/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.recorder.http.ssl

import java.util.concurrent.atomic.AtomicBoolean

import org.jboss.netty.channel.{ ChannelHandlerContext, ChannelEvent }
import org.jboss.netty.handler.ssl.SslHandler

import javax.net.ssl.SSLEngine

class FirstEventIsUnsecuredConnectSslHandler(sslEngine: SSLEngine) extends SslHandler(sslEngine, false) {

	private val sslEnabled = new AtomicBoolean(false)

	override def handleUpstream(context: ChannelHandlerContext, evt: ChannelEvent) {
		if (sslEnabled.get)
			super.handleUpstream(context, evt)
		else
			context.sendUpstream(evt)
	}

	override def handleDownstream(context: ChannelHandlerContext, evt: ChannelEvent) {
		if (sslEnabled.get)
			super.handleDownstream(context, evt);
		else {
			// enable SSL once the CONNECT request has been replied to
			sslEnabled.set(true)
			context.sendDownstream(evt)
		}
	}
}
