/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.net.InetSocketAddress;

public interface HttpListener {

  void onHttpResponse(HttpResponseStatus status, HttpHeaders headers);

  void onHttpResponseBodyChunk(ByteBuf chunk, boolean last);

  void onThrowable(Throwable e);

  default void onSend() {}

  default void onTcpConnectAttempt(InetSocketAddress remoteAddress) {}

  default void onTcpConnectSuccess(InetSocketAddress remoteAddress) {}

  default void onTcpConnectFailure(InetSocketAddress remoteAddress, Throwable cause) {}

  default void onTlsHandshakeAttempt() {}

  default void onTlsHandshakeSuccess() {}

  default void onTlsHandshakeFailure(Throwable cause) {}

  default void onProtocolAwareness(boolean isHttp2) {}

  default void onWrite(Channel channel) {}
}
