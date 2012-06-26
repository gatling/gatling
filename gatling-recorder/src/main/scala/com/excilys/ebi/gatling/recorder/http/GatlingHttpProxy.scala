/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.recorder.http

import java.net.InetSocketAddress

import org.jboss.netty.channel.Channel
import org.jboss.netty.channel.group.DefaultChannelGroup

import com.excilys.ebi.gatling.recorder.config.ProxyConfig
import com.excilys.ebi.gatling.recorder.controller.RecorderController
import com.excilys.ebi.gatling.recorder.http.channel.BootstrapFactory.newServerBootstrap

class GatlingHttpProxy(controller: RecorderController, port: Int, sslPort: Int, proxyConfig: ProxyConfig) {

	private val group = new DefaultChannelGroup("Gatling_Recorder")
	private val bootstrap = newServerBootstrap(controller, proxyConfig, false)
	private val secureBootstrap = newServerBootstrap(controller, proxyConfig, true)

	group.add(bootstrap.bind(new InetSocketAddress(port)))
	group.add(secureBootstrap.bind(new InetSocketAddress(sslPort)))

	def registerChannel(channel: Channel) {
		group.add(channel)
	}

	def shutdown {
		group.close.awaitUninterruptibly
	}
}
