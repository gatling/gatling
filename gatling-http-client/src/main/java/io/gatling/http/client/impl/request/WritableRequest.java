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

package io.gatling.http.client.impl.request;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;

public class WritableRequest {

  private final HttpRequest request;
  private final Object content;

  WritableRequest(HttpRequest request, Object content) {
    this.request = request;
    this.content = content;
  }

  public HttpRequest getRequest() {
    return request;
  }

  public Object getContent() {
    return content;
  }

  public ChannelFuture write(ChannelHandlerContext ctx) {
    if (content == null) {
      return ctx.writeAndFlush(request);
    } else {
      ctx.write(request);
      ctx.write(content);
      return ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    }
  }

  public ChannelFuture writeWithoutContent(ChannelHandlerContext ctx) {
    return ctx.writeAndFlush(request);
  }

  public ChannelFuture writeContent(ChannelHandlerContext ctx) {
    ctx.write(content);
    return ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
  }

  @Override
  public String toString() {
    return "WritableRequest{" +
      "request=" + request +
      ", content=" + content +
      '}';
  }
}
