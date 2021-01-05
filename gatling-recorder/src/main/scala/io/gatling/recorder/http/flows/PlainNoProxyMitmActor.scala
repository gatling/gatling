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

package io.gatling.recorder.http.flows

import io.gatling.commons.util.Clock
import io.gatling.recorder.http.Netty._
import io.gatling.recorder.http.TrafficLogger

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.handler.codec.http.FullHttpRequest

/**
 * Standard flow:
 * <ul>
 * <li>received request with absolute url</li>
 * <li>connect to remote host</li>
 * <li>propagate request with relative url</li>
 * <li>receive response and propagate it to serverChannel</li>
 * <li>receive new request</li>
 * <li>use existing clientChannel if it's active and connected to the same remote, close it and open a new open otherwise</li>
 * </ul>
 *
 * @param serverChannel   the server channel connected to the user agent
 * @param clientBootstrap the bootstrap to establish client channels with the remote
 * @param trafficLogger log the traffic
 */
class PlainNoProxyMitmActor(
    serverChannel: Channel,
    clientBootstrap: Bootstrap,
    trafficLogger: TrafficLogger,
    clock: Clock
) extends PlainMitmActor(serverChannel, clientBootstrap, trafficLogger, clock) {

  override protected def connectedRemote(requestRemote: Remote): Remote =
    requestRemote

  override protected def propagatedRequest(originalRequest: FullHttpRequest): FullHttpRequest =
    originalRequest.makeRelative.filterSupportedEncodings
}
