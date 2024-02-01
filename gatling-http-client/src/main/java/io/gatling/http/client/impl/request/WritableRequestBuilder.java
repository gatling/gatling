/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpMethod.*;

import io.gatling.http.client.Request;
import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.WritableContent;
import io.gatling.http.client.proxy.HttpProxyServer;
import io.gatling.http.client.proxy.ProxyServer;
import io.gatling.http.client.uri.Uri;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import java.util.function.Function;

public final class WritableRequestBuilder {

  private static String requestUrl(Uri uri, ProxyServer proxyServer, boolean http2) {
    return http2 || (proxyServer instanceof HttpProxyServer && !uri.isSecured())
        ? uri.toUrl()
        : // HTTP proxy with clear HTTP uses absolute url
        uri.toRelativeUrl();
  }

  private static WritableRequest buildRequestWithoutBody(
      HttpMethod method, String url, HttpHeaders headers) {
    // force content-length to 0 when method usually takes a body, some servers might break
    // otherwise
    if (!headers.contains(CONTENT_LENGTH)
        && (POST.equals(method) || PUT.equals(method) || PATCH.equals(method))) {
      headers.set(CONTENT_LENGTH, 0);
    }

    return new WritableRequest(
        new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            method,
            url,
            Unpooled.EMPTY_BUFFER,
            headers,
            EmptyHttpHeaders.INSTANCE),
        null);
  }

  private static WritableRequest buildFullRequest(
      HttpMethod method, String url, HttpHeaders headers, ByteBuf body, long contentLength) {
    if (!headers.contains(CONTENT_LENGTH)) {
      headers.set(CONTENT_LENGTH, contentLength);
    }

    return new WritableRequest(
        new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1, method, url, body, headers, EmptyHttpHeaders.INSTANCE),
        null);
  }

  private static WritableRequest buildRequestWithChunkedBody(
      HttpMethod method, String url, HttpHeaders headers, Object body, long contentLength) {
    if (!headers.contains(CONTENT_LENGTH) && !headers.contains(TRANSFER_ENCODING)) {
      if (contentLength >= 0) {
        headers.set(CONTENT_LENGTH, contentLength);
      } else {
        headers.set(TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
      }
    }

    return new WritableRequest(
        new DefaultHttpRequest(HttpVersion.HTTP_1_1, method, url, headers), body);
  }

  public static WritableRequest buildRequest(Request request, ByteBufAllocator alloc, boolean http2)
      throws Exception {
    return buildRequest0(signRequest(request), alloc, http2);
  }

  private static Request signRequest(Request request) {
    Function<Request, Request> signatureCalculator = request.getSignatureCalculator();
    return signatureCalculator != null
        ? signatureCalculator.apply(request.copyWithCopiedHeaders())
        : request;
  }

  private static WritableRequest buildRequest0(
      Request request, ByteBufAllocator alloc, boolean http2) throws Exception {

    HttpMethod method = request.getMethod();
    String url = requestUrl(request.getUri(), request.getProxyServer(), http2);
    HttpHeaders headers = request.getHeaders();
    RequestBody requestBody = request.getBody();

    WritableContent writableContent = requestBody != null ? requestBody.build(alloc) : null;

    if (writableContent == null) {
      return buildRequestWithoutBody(method, url, headers);

    } else {
      long contentLength = writableContent.getContentLength();
      Object content = writableContent.getContent();
      if (content instanceof ByteBuf
          && !headers.contains(EXPECT, HttpHeaderValues.CONTINUE, true)) {
        // FIXME don't handle Expect-Continue here
        return buildFullRequest(method, url, headers, (ByteBuf) content, contentLength);

      } else {
        return buildRequestWithChunkedBody(method, url, headers, content, contentLength);
      }
    }
  }
}
