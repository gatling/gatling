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
package io.gatling.recorder.http

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.controller.RecorderController
import io.gatling.recorder.http.channel.BootstrapFactory._
import io.gatling.recorder.http.ssl.SslServerContext

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.util.concurrent.GlobalEventExecutor

private[recorder] case class HttpProxy(controller: RecorderController)(implicit config: RecorderConfiguration) extends StrictLogging {

  def outgoingProxy =
    for {
      host <- config.proxy.outgoing.host
      port <- config.proxy.outgoing.port
    } yield (host, port)

  def outgoingUsername = config.proxy.outgoing.username
  def outgoingPassword = config.proxy.outgoing.password

  private val clientGroup = new NioEventLoopGroup
  private val serverBossGroup = new NioEventLoopGroup(1)
  private val serverWorkerGroup = new NioEventLoopGroup
  private val group = new DefaultChannelGroup("Gatling_Recorder", GlobalEventExecutor.INSTANCE)

  val userBootstrap = newUserBootstrap(serverBossGroup, serverWorkerGroup, this, config) // covers both http and https
  group.add(userBootstrap.bind(new InetSocketAddress(config.proxy.port)).sync.channel)

  val remoteBootstrap = newRemoteBootstrap(clientGroup, ssl = false, config)
  val secureRemoteBootstrap = newRemoteBootstrap(clientGroup, ssl = true, config)
  val sslServerContext = SslServerContext(config)

  def shutdown(): Unit = {
    group.close.awaitUninterruptibly
    clientGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
    serverBossGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
    serverWorkerGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS)
  }
}
