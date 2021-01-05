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

package io.gatling.http.client.proxy;

import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class Socks4ProxyServer extends SockProxyServer {

  private final InetSocketAddress address;
  private final String username;

  public Socks4ProxyServer(String host, int port, String username) throws UnknownHostException {
    super(host, port);
    this.address = new InetSocketAddress(host, port);
    this.username = username;
  }

  @Override
  public ProxyHandler newHandler() {
    return new Socks4ProxyHandler(address, username);
  }

  @Override
  public String toString() {
    return "Socks4ProxyServer{" +
      "address=" + address +
      ", username='" + username + '\'' +
      ", host='" + host + '\'' +
      ", port=" + port +
      '}';
  }
}
