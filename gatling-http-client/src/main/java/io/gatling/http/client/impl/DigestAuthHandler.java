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

import io.gatling.http.client.HttpClientConfig;
import io.gatling.http.client.impl.request.WritableRequest;
import io.gatling.http.client.impl.request.WritableRequestBuilder;
import io.gatling.http.client.realm.DigestRealm;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;
import static io.netty.handler.codec.http.HttpHeaderNames.WWW_AUTHENTICATE;

class DigestAuthHandler extends ChannelInboundHandlerAdapter {

  private final HttpTx tx;
  private final DigestRealm realm;
  private final HttpClientConfig config;
  private String digestHeader;

  DigestAuthHandler(HttpTx tx, DigestRealm realm, HttpClientConfig config) {
    this.tx = tx;
    this.realm = realm;
    this.config = config;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    if (digestHeader == null) {
      // initial state
      if (msg instanceof HttpResponse) {
        HttpResponse response = (HttpResponse) msg;
        if (response.status() == HttpResponseStatus.UNAUTHORIZED) {
          String authenticateHeader = getHeaderWithPrefix(response.headers().getAll(WWW_AUTHENTICATE), "Digest");
          if (authenticateHeader != null) {
            digestHeader = realm.computeAuthorizationHeader(tx.request.getMethod(), tx.request.getUri(), authenticateHeader);
            ReferenceCountUtil.release(msg);
            return;
          }
        }
      }

      // nothing this handler can do about it
      // either we don't need authentication, or auth scheme is not Digest
      ctx.fireChannelRead(msg);

    } else if (msg instanceof LastHttpContent) {
        ReferenceCountUtil.release(msg);
        // send new request
        // FIXME make sure connection can be reused, otherwise use a new one
        // FIXME check what happens if buildRequest throws
        WritableRequest request = WritableRequestBuilder.buildRequest(tx.request, ctx.alloc(), config, false);
        request.getRequest().headers().add(AUTHORIZATION, digestHeader);

        // FIXME write can throw Exception!!!
        request.write(ctx);
        ctx.pipeline().remove(this);
    } else {
      // initial response chunks are just ignored
      ReferenceCountUtil.release(msg);
    }
  }

  private static String getHeaderWithPrefix(List<String> authenticateHeaders, String prefix) {
    if (authenticateHeaders != null) {
      for (String authenticateHeader : authenticateHeaders) {
        if (authenticateHeader.regionMatches(true, 0, prefix, 0, prefix.length()))
          return authenticateHeader;
      }
    }

    return null;
  }
}
