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
import io.netty.channel.ChannelHandlerContext;

@ChannelHandler.Sharable
public class NoopHandler implements ChannelHandler {

  public static final NoopHandler INSTANCE = new NoopHandler();

  private NoopHandler() {
  }
  
  
  @Override
  public void handlerAdded(ChannelHandlerContext ctx) {
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) {
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
  }
}
