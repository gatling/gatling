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

package io.gatling.http.javaapi;

import io.gatling.commons.validation.Validation;
import io.gatling.core.javaapi.CheckBuilder;
import io.gatling.core.javaapi.Filter;
import io.gatling.core.javaapi.ProtocolBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.http.client.SignatureCalculator;
import io.gatling.http.client.uri.Uri;
import io.gatling.http.response.Response;
import scala.Function1;
import scala.Function2;
import scala.PartialFunction;
import scala.Tuple2;

import javax.net.ssl.KeyManagerFactory;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;
import static io.gatling.http.javaapi.internal.HttpChecks.*;

public final class HttpProtocolBuilder implements ProtocolBuilder {
  private final io.gatling.http.protocol.HttpProtocolBuilder wrapped;

  public HttpProtocolBuilder(io.gatling.http.protocol.HttpProtocolBuilder wrapped) {
    this.wrapped = wrapped;
  }

  public io.gatling.core.protocol.Protocol protocol() {
    return wrapped.protocol();
  }

  public HttpProtocolBuilder baseUrl(String url) {
    return baseUrls(url);
  }

  public HttpProtocolBuilder baseUrls(String... urls) {
    return baseUrls(Arrays.asList(urls));
  }

  public HttpProtocolBuilder baseUrls(List<String> urls) {
    return new HttpProtocolBuilder(wrapped.baseUrls(toScalaSeq(urls)));
  }

  public HttpProtocolBuilder warmUp(String url) {
    return new HttpProtocolBuilder(wrapped.warmUp(url));
  }

  public HttpProtocolBuilder disableWarmUp() {
    return new HttpProtocolBuilder(wrapped.disableWarmUp());
  }

  // enginePart
  public HttpProtocolBuilder shareConnections() {
    return new HttpProtocolBuilder(wrapped.shareConnections());
  }

  public HttpProtocolBuilder virtualHost(String virtualHost) {
    return new HttpProtocolBuilder(wrapped.virtualHost(toStringExpression(virtualHost)));
  }

  public HttpProtocolBuilder virtualHost(Function<Session, String> virtualHost) {
    return new HttpProtocolBuilder(wrapped.virtualHost(toTypedGatlingSessionFunction(virtualHost)));
  }

  public HttpProtocolBuilder localAddress(String address) {
    return new HttpProtocolBuilder(wrapped.localAddress(address));
  }

  public HttpProtocolBuilder localAddresses(String... addresses) {
    return new HttpProtocolBuilder(wrapped.localAddresses(toScalaSeq(addresses)));
  }

  public HttpProtocolBuilder localAddresses(List<String> addresses) {
    return new HttpProtocolBuilder(wrapped.localAddresses(toScalaSeq(addresses)));
  }

  public HttpProtocolBuilder useAllLocalAddresses() {
    return new HttpProtocolBuilder(wrapped.useAllLocalAddresses());
  }

  public HttpProtocolBuilder useAllLocalAddressesMatching(String... patterns) {
    return new HttpProtocolBuilder(wrapped.useAllLocalAddressesMatching(toScalaSeq(patterns)));
  }

  public HttpProtocolBuilder maxConnectionsPerHostLikeFirefoxOld() {
    return new HttpProtocolBuilder(wrapped.maxConnectionsPerHostLikeFirefoxOld());
  }

  public HttpProtocolBuilder maxConnectionsPerHostLikeFirefox() {
    return new HttpProtocolBuilder(wrapped.maxConnectionsPerHostLikeFirefox());
  }

  public HttpProtocolBuilder maxConnectionsPerHostLikeOperaOld() {
    return new HttpProtocolBuilder(wrapped.maxConnectionsPerHostLikeOperaOld());
  }

  public HttpProtocolBuilder maxConnectionsPerHostLikeOpera() {
    return new HttpProtocolBuilder(wrapped.maxConnectionsPerHostLikeOpera());
  }

  public HttpProtocolBuilder maxConnectionsPerHostLikeSafariOld() {
    return new HttpProtocolBuilder(wrapped.maxConnectionsPerHostLikeSafariOld());
  }

  public HttpProtocolBuilder maxConnectionsPerHostLikeSafari() {
    return new HttpProtocolBuilder(wrapped.maxConnectionsPerHostLikeSafari());
  }

  public HttpProtocolBuilder maxConnectionsPerHostLikeIE7() {
    return new HttpProtocolBuilder(wrapped.maxConnectionsPerHostLikeIE7());
  }

  public HttpProtocolBuilder maxConnectionsPerHostLikeIE8() {
    return new HttpProtocolBuilder(wrapped.maxConnectionsPerHostLikeIE8());
  }

  public HttpProtocolBuilder maxConnectionsPerHostLikeIE10() {
    return new HttpProtocolBuilder(wrapped.maxConnectionsPerHostLikeIE10());
  }

  public HttpProtocolBuilder maxConnectionsPerHostLikeChrome() {
    return new HttpProtocolBuilder(wrapped.maxConnectionsPerHostLikeChrome());
  }

  public HttpProtocolBuilder maxConnectionsPerHost(int max) {
    return new HttpProtocolBuilder(wrapped.maxConnectionsPerHost(max));
  }

  public HttpProtocolBuilder perUserKeyManagerFactory(Function<Long, KeyManagerFactory> f) {
    return new HttpProtocolBuilder(wrapped.perUserKeyManagerFactory(untyped -> f.apply((Long) untyped)));
  }

  // requestPart
  public HttpProtocolBuilder disableAutoReferer() {
    return new HttpProtocolBuilder(wrapped.disableAutoReferer());
  }

  public HttpProtocolBuilder disableAutoOrigin() {
    return new HttpProtocolBuilder(wrapped.disableAutoOrigin());
  }

  public HttpProtocolBuilder disableCaching() {
    return new HttpProtocolBuilder(wrapped.disableCaching());
  }

  public HttpProtocolBuilder header(CharSequence name, String value) {
    return new HttpProtocolBuilder(wrapped.header(name, toStringExpression(value)));
  }

  public HttpProtocolBuilder header(CharSequence name, Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.header(name, toTypedGatlingSessionFunction(value)));
  }

  public HttpProtocolBuilder headers(Map<? extends CharSequence, String> headers) {
    return new HttpProtocolBuilder(wrapped.headers(toScalaMap(headers)));
  }

  public HttpProtocolBuilder acceptHeader(String value) {
    return new HttpProtocolBuilder(wrapped.acceptHeader(toStringExpression(value)));
  }

  public HttpProtocolBuilder acceptHeader(Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.acceptHeader(toTypedGatlingSessionFunction(value)));
  }

  public HttpProtocolBuilder acceptCharsetHeader(String value) {
    return new HttpProtocolBuilder(wrapped.acceptCharsetHeader(toStringExpression(value)));
  }

  public HttpProtocolBuilder acceptCharsetHeader(Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.acceptCharsetHeader(toTypedGatlingSessionFunction(value)));
  }

  public HttpProtocolBuilder acceptEncodingHeader(String value) {
    return new HttpProtocolBuilder(wrapped.acceptEncodingHeader(toStringExpression(value)));
  }

  public HttpProtocolBuilder acceptEncodingHeader(Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.acceptEncodingHeader(toTypedGatlingSessionFunction(value)));
  }

  public HttpProtocolBuilder acceptLanguageHeader(String value) {
    return new HttpProtocolBuilder(wrapped.acceptLanguageHeader(toStringExpression(value)));
  }

  public HttpProtocolBuilder acceptLanguageHeader(Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.acceptLanguageHeader(toTypedGatlingSessionFunction(value)));
  }

  public HttpProtocolBuilder authorizationHeader(String value) {
    return new HttpProtocolBuilder(wrapped.authorizationHeader(toStringExpression(value)));
  }

  public HttpProtocolBuilder authorizationHeader(Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.authorizationHeader(toTypedGatlingSessionFunction(value)));
  }

  public HttpProtocolBuilder connectionHeader(String value) {
    return new HttpProtocolBuilder(wrapped.connectionHeader(toStringExpression(value)));
  }

  public HttpProtocolBuilder connectionHeader(Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.connectionHeader(toTypedGatlingSessionFunction(value)));
  }

  public HttpProtocolBuilder contentTypeHeader(String value) {
    return new HttpProtocolBuilder(wrapped.contentTypeHeader(toStringExpression(value)));
  }

  public HttpProtocolBuilder contentTypeHeader(Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.contentTypeHeader(toTypedGatlingSessionFunction(value)));
  }

  public HttpProtocolBuilder doNotTrackHeader(String value) {
    return new HttpProtocolBuilder(wrapped.doNotTrackHeader(toStringExpression(value)));
  }

  public HttpProtocolBuilder doNotTrackHeader(Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.doNotTrackHeader(toTypedGatlingSessionFunction(value)));
  }

  public HttpProtocolBuilder originHeader(String value) {
    return new HttpProtocolBuilder(wrapped.originHeader(toStringExpression(value)));
  }

  public HttpProtocolBuilder originHeader(Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.originHeader(toTypedGatlingSessionFunction(value)));
  }

  public HttpProtocolBuilder userAgentHeader(String value) {
    return new HttpProtocolBuilder(wrapped.userAgentHeader(toStringExpression(value)));
  }

  public HttpProtocolBuilder userAgentHeader(Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.userAgentHeader(toTypedGatlingSessionFunction(value)));
  }

  public HttpProtocolBuilder upgradeInsecureRequestsHeader(String value) {
    return new HttpProtocolBuilder(wrapped.upgradeInsecureRequestsHeader(toStringExpression(value)));
  }

  public HttpProtocolBuilder upgradeInsecureRequestsHeader(Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.upgradeInsecureRequestsHeader(toTypedGatlingSessionFunction(value)));
  }

  public HttpProtocolBuilder basicAuth(String username, String password) {
    return new HttpProtocolBuilder(wrapped.basicAuth(toStringExpression(username), toStringExpression(password)));
  }

  public HttpProtocolBuilder basicAuth(String username, Function<Session, String> password) {
    return new HttpProtocolBuilder(wrapped.basicAuth(toStringExpression(username), toTypedGatlingSessionFunction(password)));
  }

  public HttpProtocolBuilder basicAuth(Function<Session, String> username, String password) {
    return new HttpProtocolBuilder(wrapped.basicAuth(toTypedGatlingSessionFunction(username), toStringExpression(password)));
  }

  public HttpProtocolBuilder basicAuth(Function<Session, String> username, Function<Session, String> password) {
    return new HttpProtocolBuilder(wrapped.basicAuth(toTypedGatlingSessionFunction(username), toTypedGatlingSessionFunction(password)));
  }

  public HttpProtocolBuilder digestAuth(String username, String password) {
    return new HttpProtocolBuilder(wrapped.digestAuth(toStringExpression(username), toStringExpression(password)));
  }

  public HttpProtocolBuilder digestAuth(String username, Function<Session, String> password) {
    return new HttpProtocolBuilder(wrapped.digestAuth(toStringExpression(username), toTypedGatlingSessionFunction(password)));
  }

  public HttpProtocolBuilder digestAuth(Function<Session, String> username, String password) {
    return new HttpProtocolBuilder(wrapped.digestAuth(toTypedGatlingSessionFunction(username), toStringExpression(password)));
  }

  public HttpProtocolBuilder digestAuth(Function<Session, String> username, Function<Session, String> password) {
    return new HttpProtocolBuilder(wrapped.digestAuth(toTypedGatlingSessionFunction(username), toTypedGatlingSessionFunction(password)));
  }

  public HttpProtocolBuilder silentResources() {
    return new HttpProtocolBuilder(wrapped.silentResources());
  }

  public HttpProtocolBuilder silentUri(String regex) {
    return new HttpProtocolBuilder(wrapped.silentUri(regex));
  }

  public HttpProtocolBuilder disableUrlEncoding() {
    return new HttpProtocolBuilder(wrapped.disableUrlEncoding());
  }

  public HttpProtocolBuilder sign(SignatureCalculator calculator) {
    return new HttpProtocolBuilder(wrapped.sign(toStaticValueExpression(calculator)));
  }

  public HttpProtocolBuilder sign(Function<Session, SignatureCalculator> calculator) {
    return new HttpProtocolBuilder(wrapped.sign(toTypedGatlingSessionFunction(calculator)));
  }

  public HttpProtocolBuilder signWithOAuth1(String consumerKey, String clientSharedSecret, String token, String tokenSecret) {
    return new HttpProtocolBuilder(wrapped.signWithOAuth1(toStringExpression(consumerKey), toStringExpression(clientSharedSecret), toStringExpression(token), toStringExpression(tokenSecret)));
  }

  public HttpProtocolBuilder signWithOAuth1(Function<Session, String> consumerKey, Function<Session, String> clientSharedSecret, Function<Session, String> token, Function<Session, String> tokenSecret) {
    return new HttpProtocolBuilder(wrapped.signWithOAuth1(toTypedGatlingSessionFunction(consumerKey), toTypedGatlingSessionFunction(clientSharedSecret), toTypedGatlingSessionFunction(token), toTypedGatlingSessionFunction(tokenSecret)));
  }

  public HttpProtocolBuilder enableHttp2() {
    return new HttpProtocolBuilder(wrapped.enableHttp2());
  }

  public HttpProtocolBuilder http2PriorKnowledge(Map<String, Boolean> remotes) {
    return new HttpProtocolBuilder(wrapped.http2PriorKnowledge(toScalaMap(remotes.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))));
  }

  // responsePart
  public HttpProtocolBuilder disableFollowRedirect() {
    return new HttpProtocolBuilder(wrapped.disableFollowRedirect());
  }

  public HttpProtocolBuilder maxRedirects(int max) {
    return new HttpProtocolBuilder(wrapped.maxRedirects(max));
  }

  public HttpProtocolBuilder strict302Handling() {
    return new HttpProtocolBuilder(wrapped.strict302Handling());
  }

  public HttpProtocolBuilder transformResponse(BiFunction<Response, Session, Response> f) {
    return new HttpProtocolBuilder(wrapped.transformResponse(toTypedGatlingSessionFunction(f)));
  }

  public HttpProtocolBuilder check(CheckBuilder... checks) {
    return check(Arrays.asList(checks));
  }

  public HttpProtocolBuilder check(List<CheckBuilder> checks) {
    return new HttpProtocolBuilder(wrapped.check(toScalaChecks(checks)));
  }

  public UntypedCondition checkIf(Function<Session, Boolean> condition) {
    return new UntypedCondition(toUntypedGatlingSessionFunction(condition));
  }

  public UntypedCondition checkIf(String condition) {
    return new UntypedCondition(toBooleanExpression(condition));
  }

  public final class UntypedCondition {
    private final Function1<io.gatling.core.session.Session, Validation<Object>> condition;

    public UntypedCondition(Function1<io.gatling.core.session.Session, Validation<Object>> condition) {
      this.condition = condition;
    }

    public HttpProtocolBuilder then(CheckBuilder... thenChecks) {
      return then(Arrays.asList(thenChecks));
    }

    public HttpProtocolBuilder then(List<CheckBuilder> thenChecks) {
      return new HttpProtocolBuilder(wrapped.checkIf(condition, toScalaChecks(thenChecks)));
    }
  }

  public TypedCondition checkIf(BiFunction<Response, Session, Boolean> condition) {
    return new TypedCondition(toUntypedGatlingSessionFunction(condition));
  }

  public final class TypedCondition {
    private final Function2<Response, io.gatling.core.session.Session, Validation<Object>> condition;

    public TypedCondition(Function2<Response, io.gatling.core.session.Session, Validation<Object>> condition) {
      this.condition = condition;
    }

    public HttpProtocolBuilder then(CheckBuilder... thenChecks) {
      return then(Arrays.asList(thenChecks));
    }

    public HttpProtocolBuilder then(List<CheckBuilder> thenChecks) {
      return new HttpProtocolBuilder(wrapped.checkIf(condition, toScalaChecks(thenChecks)));
    }
  }

  public HttpProtocolBuilder inferHtmlResources() {
    return new HttpProtocolBuilder(wrapped.inferHtmlResources());
  }

  public HttpProtocolBuilder inferHtmlResources(Filter.AllowList allow) {
    return new HttpProtocolBuilder(wrapped.inferHtmlResources(allow.asScala()));
  }

  public HttpProtocolBuilder inferHtmlResources(Filter.AllowList allow, Filter.DenyList deny) {
    return new HttpProtocolBuilder(wrapped.inferHtmlResources(allow.asScala(), deny.asScala()));
  }

  public HttpProtocolBuilder inferHtmlResources(Filter.DenyList deny) {
    return new HttpProtocolBuilder(wrapped.inferHtmlResources(deny.asScala()));
  }

  public HttpProtocolBuilder inferHtmlResources(Filter.DenyList deny, Filter.AllowList allow) {
    return new HttpProtocolBuilder(wrapped.inferHtmlResources(deny.asScala(), allow.asScala()));
  }

  public HttpProtocolBuilder nameInferredHtmlResourcesAfterUrlTail() {
    return new HttpProtocolBuilder(wrapped.nameInferredHtmlResourcesAfterUrlTail());
  }

  public HttpProtocolBuilder nameInferredHtmlResourcesAfterAbsoluteUrl() {
    return new HttpProtocolBuilder(wrapped.nameInferredHtmlResourcesAfterAbsoluteUrl());
  }

  public HttpProtocolBuilder nameInferredHtmlResourcesAfterRelativeUrl() {
    return new HttpProtocolBuilder(wrapped.nameInferredHtmlResourcesAfterRelativeUrl());
  }

  public HttpProtocolBuilder nameInferredHtmlResourcesAfterPath() {
    return new HttpProtocolBuilder(wrapped.nameInferredHtmlResourcesAfterPath());
  }

  public HttpProtocolBuilder nameInferredHtmlResourcesAfterLastPathElement() {
    return new HttpProtocolBuilder(wrapped.nameInferredHtmlResourcesAfterLastPathElement());
  }

  public HttpProtocolBuilder nameInferredHtmlResources(Function<Uri, String> f) {
    return new HttpProtocolBuilder(wrapped.nameInferredHtmlResources(f::apply));
  }

  // wsPart
  public HttpProtocolBuilder wsBaseUrl(String url) {
    return new HttpProtocolBuilder(wrapped.wsBaseUrl(url));
  }

  public HttpProtocolBuilder wsBaseUrls(String... urls) {
    return new HttpProtocolBuilder(wrapped.wsBaseUrls(toScalaSeq(urls)));
  }

  public HttpProtocolBuilder wsBaseUrls(List<String> urls) {
    return new HttpProtocolBuilder(wrapped.wsBaseUrls(toScalaSeq(urls)));
  }

  public HttpProtocolBuilder wsReconnect() {
    return new HttpProtocolBuilder(wrapped.wsReconnect());
  }

  public HttpProtocolBuilder wsMaxReconnects(int max) {
    return new HttpProtocolBuilder(wrapped.wsMaxReconnects(max));
  }

  public HttpProtocolBuilder wsAutoReplyTextFrame(Function<String, String> f) {
    return new HttpProtocolBuilder(wrapped.wsAutoReplyTextFrame(new PartialFunction<String, String>() {
      @Override
      public boolean isDefinedAt(String x) {
        return f.apply(x) != null;
      }

      @Override
      public String apply(String v1) {
        return f.apply(v1);
      }
    }));
  }

  public HttpProtocolBuilder wsAutoReplySocketIo4() {
    return new HttpProtocolBuilder(wrapped.wsAutoReplySocketIo4());
  }

  // proxyPart
  public HttpProtocolBuilder noProxyFor(String... hosts) {
    return new HttpProtocolBuilder(wrapped.noProxyFor(toScalaSeq(hosts)));
  }

  public HttpProtocolBuilder proxy(Proxy proxy) {
    return new HttpProtocolBuilder(wrapped.proxy(proxy.asScala()));
  }

  // dnsPart
  public HttpProtocolBuilder asyncNameResolution(String... dnsServers) {
    return new HttpProtocolBuilder(wrapped.asyncNameResolution(toScalaSeq(dnsServers)));
  }

  public HttpProtocolBuilder asyncNameResolution(InetSocketAddress[] dnsServers) {
    return new HttpProtocolBuilder(wrapped.asyncNameResolution(dnsServers));
  }

  public HttpProtocolBuilder perUserNameResolution() {
    return new HttpProtocolBuilder(wrapped.perUserNameResolution());
  }
}
