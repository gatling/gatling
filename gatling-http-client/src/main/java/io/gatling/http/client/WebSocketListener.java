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

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.*;

public abstract class WebSocketListener implements HttpListener, WebSocket {

  private volatile Channel channel;

  public void openWebSocket(Channel channel) {
    this.channel = channel;
    onWebSocketOpen();
  }

  public abstract void onWebSocketOpen();

  public abstract void onTextFrame(TextWebSocketFrame frame);

  public abstract void onBinaryFrame(BinaryWebSocketFrame frame);

  public abstract void onPongFrame(PongWebSocketFrame frame);

  public abstract void onCloseFrame(CloseWebSocketFrame frame);

  @Override
  public void sendFrame(WebSocketFrame frame) {
    channel.writeAndFlush(frame);
  }

  @Override
  public void onHttpResponseBodyChunk(ByteBuf chunk, boolean last) {
  }
}
