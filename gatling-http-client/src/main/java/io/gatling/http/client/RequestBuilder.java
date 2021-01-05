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

import io.gatling.http.client.resolver.InetAddressNameResolver;
import io.gatling.http.client.uri.Uri;
import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;
import io.gatling.http.client.proxy.ProxyServer;
import io.gatling.http.client.realm.BasicRealm;
import io.gatling.http.client.realm.Realm;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cookie.ClientCookieEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.util.AsciiString;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import static io.gatling.http.client.util.HttpUtils.*;
import static io.gatling.http.client.util.MiscUtils.isNonEmpty;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class RequestBuilder {

  private static final AsciiString ACCEPT_ALL_HEADER_VALUE = new AsciiString("*/*");

  private final HttpMethod method;
  private final Uri uri;
  private final InetAddressNameResolver nameResolver;
  private HttpHeaders headers = new DefaultHttpHeaders(false);
  private List<Cookie> cookies;
  private RequestBodyBuilder bodyBuilder;
  private long requestTimeout;
  private String virtualHost;
  private InetAddress localIpV4Address;
  private InetAddress localIpV6Address;
  private Realm realm;
  private ProxyServer proxyServer;
  private SignatureCalculator signatureCalculator;
  private boolean http2Enabled;
  private boolean alpnRequired;
  private boolean http2PriorKnowledge;
  private String wsSubprotocol;
  private Charset defaultCharset = UTF_8;

  public RequestBuilder(HttpMethod method, Uri uri, InetAddressNameResolver nameResolver) {
    this.method = method;
    this.uri = uri;
    this.nameResolver = nameResolver;
  }

  public RequestBuilder(Request request, Uri uri) {
    this(request.getMethod(), uri, request.getNameResolver());
    headers = request.getHeaders();
    cookies = request.getCookies();
    bodyBuilder = request.getBody() != null ? request.getBody().newBuilder() : null;
    requestTimeout = request.getRequestTimeout();
    virtualHost = request.getVirtualHost();
    localIpV4Address = request.getLocalIpV4Address();
    localIpV6Address = request.getLocalIpV6Address();
    realm = request.getRealm();
    proxyServer = request.getProxyServer();
    signatureCalculator = request.getSignatureCalculator();
    http2Enabled = request.isHttp2Enabled();
    alpnRequired = request.isAlpnRequired();
    http2PriorKnowledge = request.isHttp2PriorKnowledge();
    wsSubprotocol = request.getWsSubprotocol();
  }

  public Uri getUri() {
    return uri;
  }

  public RequestBuilder setHeaders(HttpHeaders headers) {
    this.headers = headers;
    return this;
  }

  public RequestBuilder addHeader(CharSequence name, Object value) {
    this.headers.add(name, value);
    return this;
  }

  public RequestBuilder setCookies(List<Cookie> cookies) {
    this.cookies = cookies;
    return this;
  }

  public RequestBuilder setBodyBuilder(RequestBodyBuilder bodyBuilder) {
    this.bodyBuilder = bodyBuilder;
    return this;
  }

  public RequestBuilder setRequestTimeout(long requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  public RequestBuilder setVirtualHost(String virtualHost) {
    this.virtualHost = virtualHost;
    return this;
  }

  public RequestBuilder setLocalIpV4Address(InetAddress localIpV4Address) {
    this.localIpV4Address = localIpV4Address;
    return this;
  }

  public RequestBuilder setLocalIpV6Address(InetAddress localIpV6Address) {
    this.localIpV6Address = localIpV6Address;
    return this;
  }

  public RequestBuilder setRealm(Realm realm) {
    this.realm = realm;
    return this;
  }

  public RequestBuilder setProxyServer(ProxyServer proxyServer) {
    this.proxyServer = proxyServer;
    return this;
  }

  public RequestBuilder setSignatureCalculator(SignatureCalculator signatureCalculator) {
    this.signatureCalculator = signatureCalculator;
    return this;
  }

  public RequestBuilder setHttp2Enabled(boolean http2Enabled) {
    this.http2Enabled = http2Enabled;
    return this;
  }

  public RequestBuilder setAlpnRequired(boolean alpnRequired) {
    this.alpnRequired = alpnRequired;
    return this;
  }

  public RequestBuilder setHttp2PriorKnowledge(boolean http2PriorKnowledge) {
    this.http2PriorKnowledge = http2PriorKnowledge;
    return this;
  }

  public RequestBuilder setDefaultCharset(Charset defaultCharset) {
    this.defaultCharset = defaultCharset;
    return this;
  }

  public RequestBuilder setWsSubprotocol(String wsSubprotocol) {
    this.wsSubprotocol = wsSubprotocol;
    return this;
  }

  public Request build() {

    if (!headers.contains(ACCEPT)) {
      headers.set(ACCEPT, ACCEPT_ALL_HEADER_VALUE);
    }

    if (realm instanceof BasicRealm) {
      headers.add(AUTHORIZATION, ((BasicRealm) realm).getAuthorizationHeader());
    }

    String originalAcceptEncoding = headers.get(ACCEPT_ENCODING);
    if (originalAcceptEncoding != null) {
      // we don't support Brotly ATM
      String newAcceptEncodingHeader = filterOutBrotliFromAcceptEncoding(originalAcceptEncoding);
      if (newAcceptEncodingHeader != null) {
        headers.set(ACCEPT_ENCODING, newAcceptEncodingHeader);
      }
    }

    if (isNonEmpty(cookies)) {
      headers.set(COOKIE, ClientCookieEncoder.LAX.encode(cookies));
    }

    String referer = headers.get(REFERER);
    if (referer != null
      && !HttpMethod.GET.equals(method)
      && !HttpMethod.HEAD.equals(method)
      && !headers.contains(ORIGIN)) {
      String origin = originHeader(referer);
      if (origin != null) {
        headers.set(ORIGIN, origin);
      }
    }

    if (!headers.contains(HOST)) {
      headers.set(HOST, virtualHost != null ? virtualHost : hostHeader(uri));
    }

    RequestBody body = null;
    if (bodyBuilder != null) {
      String contentType = headers.get(CONTENT_TYPE);
      Charset charset = extractContentTypeCharsetAttribute(contentType);
      body = bodyBuilder.build(contentType, charset, defaultCharset);
      String bodyContentType = body.getContentType();
      if (bodyContentType != null) {
        headers.set(CONTENT_TYPE, bodyContentType);
      }
    }

    return new Request(
      method,
      uri,
      headers,
      cookies != null ? cookies : Collections.emptyList(),
      body,
      requestTimeout,
      virtualHost,
      localIpV4Address,
      localIpV6Address,
      realm,
      proxyServer,
      signatureCalculator,
      nameResolver,
      http2Enabled,
      alpnRequired,
      http2PriorKnowledge,
      wsSubprotocol
      );
  }
}
