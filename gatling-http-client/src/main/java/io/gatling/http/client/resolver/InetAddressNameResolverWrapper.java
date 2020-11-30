/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.http.client.resolver;

import io.gatling.http.client.HttpListener;
import io.netty.resolver.NameResolver;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.net.InetAddress;
import java.util.List;

public class InetAddressNameResolverWrapper implements InetAddressNameResolver {

  private final NameResolver<InetAddress> wrapped;

  public InetAddressNameResolverWrapper(NameResolver<InetAddress> wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public Future<List<InetAddress>> resolveAll(String inetHost, Promise<List<InetAddress>> promise, HttpListener listener) {
    return wrapped.resolveAll(inetHost, promise);
  }

  @Override
  public void close() {
    wrapped.close();
  }
}
