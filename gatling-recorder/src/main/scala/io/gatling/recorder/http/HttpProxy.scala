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
package io.gatling.recorder.http

import java.net.InetSocketAddress

import org.jboss.netty.channel.group.DefaultChannelGroup

import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.channel.BootstrapFactory.{ newClientBootstrap, newServerBootstrap }

case class HttpProxy(controller: RecorderController, port: Int, sslPort: Int) {

	private val group = new DefaultChannelGroup("Gatling_Recorder")
	val clientBootstrap = newClientBootstrap(false)
	val secureClientBootstrap = newClientBootstrap(true)
	private val serverBootstrap = newServerBootstrap(this, false)
	private val secureServerBootstrap = newServerBootstrap(this, true)

	group.add(serverBootstrap.bind(new InetSocketAddress(port)))
	group.add(secureServerBootstrap.bind(new InetSocketAddress(sslPort)))

	def shutdown {
		group.close.awaitUninterruptibly
		serverBootstrap.shutdown
		secureClientBootstrap.shutdown
		clientBootstrap.shutdown
		secureClientBootstrap.shutdown
	}
}
