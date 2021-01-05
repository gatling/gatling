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
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class HttpProxyServer extends ProxyServer {

  private final BasicRealm realm;
  private final int securedPort;
  private final InetSocketAddress securedAddress;

  public HttpProxyServer(String host, int port, int securedPort, BasicRealm realm) throws UnknownHostException {
    super(host, port);
    this.securedPort  = securedPort;
    this.realm = realm;
    this.securedAddress = new InetSocketAddress(inetAddress, securedPort);
  }

  public int getSecuredPort() {
    return securedPort;
  }

  public BasicRealm getRealm() {
    return realm;
  }

  @Override
  public ProxyHandler newHandler() {
    return realm != null ?
            new HttpProxyHandler(securedAddress, realm.getUsername(), realm.getPassword(), EmptyHttpHeaders.INSTANCE, true) :
            new HttpProxyHandler(securedAddress, EmptyHttpHeaders.INSTANCE, true);
  }

  @Override
  public String toString() {
    return "HttpProxyServer{" +
      "realm=" + realm +
      ", securedPort=" + securedPort +
      ", securedAddress=" + securedAddress +
      ", host='" + host + '\'' +
      ", port=" + port +
      '}';
  }
}
