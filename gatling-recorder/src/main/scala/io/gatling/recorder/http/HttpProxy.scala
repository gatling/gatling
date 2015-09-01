/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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
package io.gatling.recorder.http

import java.net.InetSocketAddress

import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.channel.BootstrapFactory.{ newRemoteBootstrap, newUserBootstrap }
import io.gatling.recorder.http.ssl.SslServerContext
import org.jboss.netty.channel.group.DefaultChannelGroup

private[recorder] case class HttpProxy(controller: RecorderController)(implicit config: RecorderConfiguration) {

  private def port = config.proxy.port
  def outgoingProxy =
    for {
      host <- config.proxy.outgoing.host
      port <- config.proxy.outgoing.port
    } yield (host, port)

  def outgoingUsername = config.proxy.outgoing.username
  def outgoingPassword = config.proxy.outgoing.password

  val remoteBootstrap = newRemoteBootstrap(ssl = false, config)
  val secureRemoteBootstrap = newRemoteBootstrap(ssl = true, config)
  private val group = new DefaultChannelGroup("Gatling_Recorder")
  private val userBootstrap = newUserBootstrap(this, config) // covers both http and https

  group.add(userBootstrap.bind(new InetSocketAddress(port)))

  def shutdown(): Unit = {
    group.close.awaitUninterruptibly
    userBootstrap.shutdown()
    remoteBootstrap.shutdown()
    secureRemoteBootstrap.shutdown()
  }

  val sslServerContext = SslServerContext(config)
}
