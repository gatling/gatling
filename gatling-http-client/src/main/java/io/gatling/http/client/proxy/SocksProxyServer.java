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

import io.gatling.http.client.realm.BasicRealm;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import java.net.UnknownHostException;

public final class SocksProxyServer extends ProxyServer {

  private final boolean socks5;

  public SocksProxyServer(String host, int port, BasicRealm realm, boolean socks5)
      throws UnknownHostException {
    super(host, port, realm);
    this.socks5 = socks5;
  }

  @Override
  public ProxyHandler newProxyHandler() {
    if (socks5) {
      return realm != null
          ? new Socks5ProxyHandler(address, realm.getUsername(), realm.getPassword())
          : new Socks5ProxyHandler(address);
    } else {
      return realm != null
          ? new Socks4ProxyHandler(address, realm.getUsername())
          : new Socks4ProxyHandler(address);
    }
  }

  @Override
  public String toString() {
    return "SocksProxyServer{"
        + "socks5="
        + socks5
        + ", host='"
        + host
        + '\''
        + ", port="
        + port
        + ", realm="
        + realm
        + '}';
  }
}
