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
import io.netty.resolver.DefaultNameResolver;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

import java.net.InetAddress;
import java.util.List;

public interface InetAddressNameResolver extends AutoCloseable {

  InetAddressNameResolver DEFAULT = new InetAddressNameResolverWrapper(new DefaultNameResolver(ImmediateEventExecutor.INSTANCE));

  Future<List<InetAddress>> resolveAll(String inetHost, Promise<List<InetAddress>> promise, HttpListener listener);
}
