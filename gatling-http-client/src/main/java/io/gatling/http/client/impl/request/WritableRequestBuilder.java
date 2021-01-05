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

import io.gatling.http.client.HttpClientConfig;
import io.gatling.http.client.Request;
import io.gatling.http.client.RequestBuilder;
import io.gatling.http.client.SignatureCalculator;
import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.WritableContent;
import io.gatling.http.client.proxy.HttpProxyServer;
import io.gatling.http.client.uri.Uri;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

import java.io.IOException;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpMethod.*;

public class WritableRequestBuilder {

  private static WritableRequest buildRequestWithoutBody(String url,
                                                         HttpMethod method,
                                                         HttpHeaders headers) {

    // force content-length to 0 when method usually takes a body, some servers might break otherwise
    if (!headers.contains(CONTENT_LENGTH) && (POST.equals(method) || PUT.equals(method) || PATCH.equals(method))) {
      headers.set(CONTENT_LENGTH, 0);
    }

    FullHttpRequest nettyRequest = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            method,
            url,
            Unpooled.buffer(0),
            headers,
            EmptyHttpHeaders.INSTANCE);

    return new WritableRequest(nettyRequest, null);
  }

  private static WritableRequest buildRequestWithBody(String url,
                                                      HttpMethod method,
                                                      HttpHeaders headers,
                                                      RequestBody requestBody,
                                                      ByteBufAllocator alloc) throws IOException {

    WritableContent writableContent = requestBody.build(alloc);

    Object content = writableContent.getContent();

    if (content instanceof ByteBuf && !headers.contains(EXPECT, HttpHeaderValues.CONTINUE, true)) {
      ByteBuf bb = (ByteBuf) content;
      if (!headers.contains(CONTENT_LENGTH)) {
        headers.set(CONTENT_LENGTH, bb.readableBytes());
      }
      FullHttpRequest nettyRequest = new DefaultFullHttpRequest(
              HttpVersion.HTTP_1_1,
              method,
              url,
              bb,
              headers,
              EmptyHttpHeaders.INSTANCE);

      return new WritableRequest(nettyRequest, null);

    } else {
      if (!headers.contains(CONTENT_LENGTH) && !headers.contains(TRANSFER_ENCODING)) {
        if (writableContent.getContentLength() >= 0) {
          headers.set(CONTENT_LENGTH, writableContent.getContentLength());
        } else {
          headers.set(TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
        }
      }

      HttpRequest nettyRequest = new DefaultHttpRequest(
              HttpVersion.HTTP_1_1,
              method,
              url,
              headers);

      return new WritableRequest(nettyRequest, content);
    }
  }

  public static WritableRequest buildRequest(Request request, ByteBufAllocator alloc, HttpClientConfig config, boolean http2) throws Exception {
    Uri uri = request.getUri();
    HttpHeaders headers = request.getHeaders();
    RequestBody requestBody = request.getBody();

    boolean isClearHttpProxy = !uri.isSecured() && request.getProxyServer() instanceof HttpProxyServer;

    String url = http2 || isClearHttpProxy ?
            uri.toUrl() : // HTTP proxy with clear HTTP uses absolute url
            uri.toRelativeUrl();

    if (isClearHttpProxy) {
      HttpProxyServer proxyServer = (HttpProxyServer) request.getProxyServer();
      if (proxyServer.getRealm() != null) {
        headers.set(PROXY_AUTHORIZATION, proxyServer.getRealm().getAuthorizationHeader());
      }
    }

    WritableRequest writableRequest =
      requestBody == null ?
            buildRequestWithoutBody(url, request.getMethod(), headers) :
            buildRequestWithBody(url, request.getMethod(), headers, requestBody, alloc);


    SignatureCalculator signatureCalculator = request.getSignatureCalculator();
    if (signatureCalculator != null) {
      Request requestWithCompletedHeaders = new RequestBuilder(request, request.getUri())
        .setHeaders(writableRequest.getRequest().headers())
        .setDefaultCharset(config.getDefaultCharset())
        .build();
      signatureCalculator.sign(requestWithCompletedHeaders);
    }
    return writableRequest;
  }
}
