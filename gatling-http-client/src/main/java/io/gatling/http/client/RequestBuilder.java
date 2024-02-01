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

package io.gatling.http.client;

import static io.gatling.http.client.util.HttpUtils.*;
import static io.gatling.http.client.util.MiscUtils.isNonEmpty;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;
import io.gatling.http.client.proxy.HttpProxyServer;
import io.gatling.http.client.proxy.ProxyServer;
import io.gatling.http.client.realm.BasicRealm;
import io.gatling.http.client.realm.DigestRealm;
import io.gatling.http.client.realm.Realm;
import io.gatling.http.client.resolver.InetAddressNameResolver;
import io.gatling.http.client.uri.Uri;
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
import java.util.function.Function;

public final class RequestBuilder {

  private static final AsciiString ACCEPT_ALL_HEADER_VALUE = new AsciiString("*/*");

  private final String name;
  private final HttpMethod method;
  private final Uri uri;
  private final InetAddressNameResolver nameResolver;
  private HttpHeaders headers = new DefaultHttpHeaders(false);
  private List<Cookie> cookies = Collections.emptyList();
  private RequestBodyBuilder bodyBuilder;
  private long requestTimeout;
  private String virtualHost;
  private boolean autoOrigin;
  private InetAddress localIpV4Address;
  private InetAddress localIpV6Address;
  private Realm realm;
  private ProxyServer proxyServer;
  private Function<Request, Request> signatureCalculator;
  private boolean http2Enabled;
  private Http2PriorKnowledge http2PriorKnowledge;
  private String wsSubprotocol;
  private Charset defaultCharset = UTF_8;

  public RequestBuilder(
      String name, HttpMethod method, Uri uri, InetAddressNameResolver nameResolver) {
    this.name = name;
    this.method = method;
    this.uri = uri;
    this.nameResolver = nameResolver;
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

  public RequestBuilder setAutoOrigin(boolean autoOrigin) {
    this.autoOrigin = autoOrigin;
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

  public RequestBuilder setSignatureCalculator(Function<Request, Request> signatureCalculator) {
    this.signatureCalculator = signatureCalculator;
    return this;
  }

  public RequestBuilder setHttp2Enabled(boolean http2Enabled) {
    this.http2Enabled = http2Enabled;
    return this;
  }

  public RequestBuilder setHttp2PriorKnowledge(Http2PriorKnowledge http2PriorKnowledge) {
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

  public String getContentType() {
    return headers.get(CONTENT_TYPE);
  }

  public Request build() {
    if (!headers.contains(ACCEPT)) {
      headers.set(ACCEPT, ACCEPT_ALL_HEADER_VALUE);
    }

    String authorization = null;
    if (realm instanceof BasicRealm) {
      authorization = ((BasicRealm) realm).getAuthorizationHeader();
    } else if (realm instanceof DigestRealm) {
      authorization = ((DigestRealm) realm).getAuthorizationHeader(method, uri);
    }
    if (authorization != null) {
      headers.add(AUTHORIZATION, authorization);
    }

    if (!uri.isSecured() && proxyServer instanceof HttpProxyServer) {
      BasicRealm realm = ((HttpProxyServer) proxyServer).getRealm();
      if (realm != null) {
        headers.set(PROXY_AUTHORIZATION, realm.getAuthorizationHeader());
      }
    }

    String originalAcceptEncoding = headers.get(ACCEPT_ENCODING);
    if (originalAcceptEncoding != null) {
      String newAcceptEncodingHeader =
          filterOutBrotliFromAcceptEncodingWhenUnavailable(originalAcceptEncoding);
      if (newAcceptEncodingHeader != null) {
        headers.set(ACCEPT_ENCODING, newAcceptEncodingHeader);
      }
    }

    if (isNonEmpty(cookies)) {
      headers.set(COOKIE, ClientCookieEncoder.LAX.encode(cookies));
    }

    if (autoOrigin) {
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
    }

    if (!headers.contains(HOST)) {
      headers.set(HOST, virtualHost != null ? virtualHost : hostHeader(uri));
    }

    RequestBody body = null;
    if (bodyBuilder != null) {
      String contentType = getContentType();
      Charset charset = extractContentTypeCharsetAttribute(contentType);
      body = bodyBuilder.build(contentType, charset, defaultCharset);
      CharSequence patchedContentType = body.getPatchedContentType();
      if (patchedContentType != null) {
        headers.set(CONTENT_TYPE, patchedContentType);
      }
    }

    return new Request(
        name,
        method,
        uri,
        headers,
        cookies != null ? cookies : Collections.emptyList(),
        body,
        requestTimeout,
        virtualHost,
        autoOrigin,
        localIpV4Address,
        localIpV6Address,
        realm,
        proxyServer,
        signatureCalculator,
        nameResolver,
        http2Enabled,
        http2PriorKnowledge,
        wsSubprotocol);
  }
}
