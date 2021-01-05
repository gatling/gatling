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

package io.gatling.http.client.impl;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.DeflateFrameClientExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateClientExtensionHandshaker;

import static io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateServerExtensionHandshaker.MAX_WINDOW_SIZE;

@ChannelHandler.Sharable
// fork of WebSocketClientCompressionHandler that enables allowClientNoContext
public class AllowClientNoContextWebSocketClientCompressionHandler extends WebSocketClientExtensionHandler {

  public static final AllowClientNoContextWebSocketClientCompressionHandler INSTANCE = new AllowClientNoContextWebSocketClientCompressionHandler();

  private AllowClientNoContextWebSocketClientCompressionHandler() {
    super(new PerMessageDeflateClientExtensionHandshaker(6, ZlibCodecFactory.isSupportingWindowSizeAndMemLevel(), MAX_WINDOW_SIZE, true, false),
      new DeflateFrameClientExtensionHandshaker(false),
      new DeflateFrameClientExtensionHandshaker(true));
  }
}
