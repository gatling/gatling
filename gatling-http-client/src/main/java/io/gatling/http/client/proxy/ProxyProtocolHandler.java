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

package io.gatling.http.client.proxy;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.InternetProtocolFamily;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProxyProtocolHandler extends ChannelDuplexHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyProtocolHandler.class);

  private final String sourceIpV4Address;
  private final String sourceIpV6Address;

  public ProxyProtocolHandler(String sourceIpV4Address, String sourceIpV6Address) {
    this.sourceIpV4Address = sourceIpV4Address;
    this.sourceIpV6Address = sourceIpV6Address;
  }

  private String version1Header(
      InternetProtocolFamily internetProtocolFamily,
      int sourcePort,
      InetSocketAddress destination) {
    StringBuilder sb = new StringBuilder("PROXY TCP");
    if (internetProtocolFamily == InternetProtocolFamily.IPv4) {
      if (sourceIpV4Address == null) {
        return null;
      }
      sb.append("4 ").append(sourceIpV4Address);
    } else {
      if (sourceIpV6Address == null) {
        return null;
      }
      sb.append("6 ").append(sourceIpV6Address);
    }
    return sb.append(" ")
        .append(destination.getAddress().getHostAddress())
        .append(" ")
        .append(sourcePort)
        .append(" ")
        .append(destination.getPort())
        .append("\r\n")
        .toString();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    InetSocketAddress localSocketAddress = (InetSocketAddress) ctx.channel().localAddress();
    InetSocketAddress remoteSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();

    InternetProtocolFamily internetProtocolFamily =
        InternetProtocolFamily.of(localSocketAddress.getAddress());
    int sourcePort = ThreadLocalRandom.current().nextInt(1001, 64 * 1024);
    String header = version1Header(internetProtocolFamily, sourcePort, remoteSocketAddress);
    if (header == null) {
      LOGGER.debug(
          "Skipping Proxy Protocol header generation because no fake source address was provided for protocol {}",
          internetProtocolFamily);
    } else {
      LOGGER.debug(
          "Sending Proxy Protocol header {} for protocol {}", header, internetProtocolFamily);
      ctx.write(Unpooled.wrappedBuffer(header.getBytes(StandardCharsets.US_ASCII)));
    }

    ctx.pipeline().remove(this);
    super.channelActive(ctx);
  }
}
