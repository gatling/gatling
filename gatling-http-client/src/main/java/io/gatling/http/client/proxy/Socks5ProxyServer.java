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

import io.gatling.http.client.realm.BasicRealm;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;

import java.net.UnknownHostException;

public class Socks5ProxyServer extends SockProxyServer {

  private final BasicRealm realm;

  public Socks5ProxyServer(String host, int port, BasicRealm realm) throws UnknownHostException {
    super(host, port);
    this.realm = realm;
  }

  @Override
  public ProxyHandler newHandler() {
    return realm != null ? new Socks5ProxyHandler(address, realm.getUsername(), realm.getPassword()) : new Socks5ProxyHandler(address);
  }

  @Override
  public String toString() {
    return "Socks5ProxyServer{" +
      "realm=" + realm +
      ", host='" + host + '\'' +
      ", port=" + port +
      '}';
  }
}
