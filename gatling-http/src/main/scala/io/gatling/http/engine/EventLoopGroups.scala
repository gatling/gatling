/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.http.engine

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory

private[gatling] trait EventLoopGroups {

  protected def newEventLoopGroup(system: ActorSystem, poolName: String, useNativeTransport: Boolean): EventLoopGroup = {
    val eventLoopGroup = new NioEventLoopGroup(0, new DefaultThreadFactory(poolName))
    system.registerOnTermination(eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS))
    eventLoopGroup
  }
}
