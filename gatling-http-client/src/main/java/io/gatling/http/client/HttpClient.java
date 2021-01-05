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

package io.gatling.http.client;

import io.gatling.http.client.util.Pair;
import io.netty.channel.EventLoop;
import io.netty.handler.ssl.SslContext;

public interface HttpClient extends AutoCloseable {

  void sendRequest(Request request, long clientId, EventLoop eventLoop, HttpListener listener, SslContext sslContext, SslContext alpnSslContext);

  void sendHttp2Requests(Pair<Request, HttpListener>[] requestsAndListeners, long clientId, EventLoop eventLoop, SslContext sslContext, SslContext alpnSslContext);

  boolean isClosed();

  void flushClientIdChannels(long clientId, EventLoop eventLoop);
}
