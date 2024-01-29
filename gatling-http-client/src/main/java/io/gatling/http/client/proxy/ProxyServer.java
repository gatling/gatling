/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public abstract class ProxyServer {

  protected final String host;
  protected final int port;
  protected final BasicRealm realm;
  protected final InetSocketAddress address;

  ProxyServer(String host, int port, BasicRealm realm) throws UnknownHostException {
    this.host = host;
    this.port = port;
    this.realm = realm;
    this.address = new InetSocketAddress(InetAddress.getByName(host), port);
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  public abstract ProxyHandler newProxyHandler();
}
