/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.http.client.test;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/** Relays everything a proxy tunnel receives to its peer, the other side of the tunnel. */
final class TcpRelay extends ChannelInboundHandlerAdapter {
  private final Channel peer;

  TcpRelay(Channel peer) {
    this.peer = peer;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    peer.writeAndFlush(msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) {
    peer.close();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    ctx.close();
  }
}
