/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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
import io.gatling.http.client.SignatureCalculator;
import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.WritableContent;
import io.gatling.http.client.proxy.HttpProxyServer;
import io.gatling.http.client.realm.BasicRealm;
import io.gatling.http.client.realm.Realm;
import io.gatling.http.client.ahc.uri.Uri;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.util.AsciiString;

import java.io.IOException;
import java.nio.charset.Charset;

import static io.gatling.http.client.ahc.util.HttpUtils.*;
import static io.gatling.http.client.ahc.util.MiscUtils.isNonEmpty;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpMethod.*;

public class WritableRequestBuilder {

  private static final AsciiString ACCEPT_ALL_HEADER_VALUE = new AsciiString("*/*");

  private static WritableRequest buildRequestWithoutBody(String url,
                                                         HttpMethod method,
                                                         HttpHeaders headers) {

    // force content-length to 0 when method usually takes a body, some servers might break otherwise
    if (!headers.contains(CONTENT_LENGTH) && (method == POST || method == PUT || method == PATCH)) {
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
                                                      Uri uri,
                                                      HttpMethod method,
                                                      HttpHeaders headers,
                                                      RequestBody<?> requestBody,
                                                      ByteBufAllocator alloc,
                                                      HttpClientConfig config) throws IOException {
    Charset charset = config.getDefaultCharset();

    String contentTypeHeader = headers.get(CONTENT_TYPE);
    if (contentTypeHeader != null) {
      Charset contentTypeCharset = extractContentTypeCharsetAttribute(contentTypeHeader);
      if (contentTypeCharset != null) {
        charset = contentTypeCharset;
      } else {
        // set Content-Type header missing charset attribute
        contentTypeHeader = contentTypeHeader + "; charset=" + charset.name();
        headers.set(CONTENT_TYPE, contentTypeHeader);
      }
    }

    boolean zeroCopy = !uri.isSecured() && config.isEnableZeroCopy();
    WritableContent writableContent = requestBody.build(contentTypeHeader, charset, zeroCopy, alloc);

    if (contentTypeHeader == null && writableContent.getContentTypeOverride() != null) {
      headers.set(CONTENT_TYPE, writableContent.getContentTypeOverride());
    }

    Object content = writableContent.getContent();

    if (content instanceof ByteBuf) {
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

  public static WritableRequest buildRequest(Request request, ByteBufAllocator alloc, HttpClientConfig config) throws Exception {
    Uri uri = request.getUri();
    HttpHeaders headers = request.getHeaders();
    RequestBody<?> requestBody = request.getBody();
    Realm realm = request.getRealm();

    if (!headers.contains(ACCEPT)) {
      headers.set(ACCEPT, ACCEPT_ALL_HEADER_VALUE);
    }

    if (realm instanceof BasicRealm) {
      headers.add(AUTHORIZATION, ((BasicRealm) realm).getAuthorizationHeader());
    }

    String userDefinedAcceptEncoding = headers.get(ACCEPT_ENCODING);
    if (userDefinedAcceptEncoding != null) {
      // we don't support Brotly ATM
      headers.set(ACCEPT_ENCODING, filterOutBrotliFromAcceptEncoding(userDefinedAcceptEncoding));
    }

    if (isNonEmpty(request.getCookies())) {
      headers.set(COOKIE, ClientCookieEncoder.LAX.encode(request.getCookies()));
    }

    if (!headers.contains(ORIGIN)) {
      headers.set(ORIGIN, originHeader(uri));
    }

    if (!headers.contains(HOST)) {
      String virtualHost = request.getVirtualHost();
      headers.set(HOST, virtualHost != null ? virtualHost : hostHeader(uri));
    }

    String url = (uri.isSecured() && request.isHttp2Enabled()) || (!uri.isSecured() && request.getProxyServer() instanceof HttpProxyServer) ?
            uri.toUrl() : // HTTP proxy with clear HTTP uses absolute url
            uri.toRelativeUrl();

    WritableRequest writableRequest =
      requestBody == null ?
            buildRequestWithoutBody(url, request.getMethod(), headers) :
            buildRequestWithBody(url, uri, request.getMethod(), headers, requestBody, alloc, config);

    SignatureCalculator signatureCalculator = request.getSignatureCalculator();
    if (signatureCalculator != null) {
      signatureCalculator.sign(request.getMethod(), request.getUri(), writableRequest.getRequest().headers(), requestBody);
    }
    return writableRequest;
  }
}
