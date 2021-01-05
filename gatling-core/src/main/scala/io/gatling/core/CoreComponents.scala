/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

package io.gatling.core

import io.gatling.commons.util.Clock
import io.gatling.core.action.Action
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.stats.StatsEngine

import _root_.akka.actor.{ ActorRef, ActorSystem }
import io.netty.channel.EventLoopGroup

final class CoreComponents(
    val actorSystem: ActorSystem,
    val eventLoopGroup: EventLoopGroup,
    val controller: ActorRef,
    val throttler: Option[Throttler],
    val statsEngine: StatsEngine,
    val clock: Clock,
    val exit: Action,
    val configuration: GatlingConfiguration
)
