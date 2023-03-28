/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.http;

import static io.gatling.javaapi.core.internal.Converters.*;
import static io.gatling.javaapi.core.internal.Expressions.*;
import static io.gatling.javaapi.http.internal.HttpChecks.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.http.client.Request;
import io.gatling.http.client.uri.Uri;
import io.gatling.http.response.Response;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.Filter;
import io.gatling.javaapi.core.ProtocolBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.internal.HttpProtocolBuilders;
import io.gatling.javaapi.http.internal.ScalaHttpProtocolBuilderConditions;
import io.gatling.javaapi.http.internal.SignatureCalculators;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.net.ssl.KeyManagerFactory;
import scala.PartialFunction;

/**
 * DSL for building HTTP protocol configurations
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class HttpProtocolBuilder implements ProtocolBuilder {
  private final io.gatling.http.protocol.HttpProtocolBuilder wrapped;

  public HttpProtocolBuilder(io.gatling.http.protocol.HttpProtocolBuilder wrapped) {
    this.wrapped = wrapped;
  }

  public io.gatling.core.protocol.Protocol protocol() {
    return wrapped.protocol();
  }

  /**
   * Define the baseUrl that will be used as a prefix for all relative urls
   *
   * @param url the base url
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder baseUrl(@NonNull String url) {
    return baseUrls(url);
  }

  /**
   * Define multiple baseUrls that will be used as a prefix for all relative urls. Assigned once per
   * virtual user based on a round-robin strategy.
   *
   * @param urls the base urls
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder baseUrls(@NonNull String... urls) {
    return baseUrls(Arrays.asList(urls));
  }

  /**
   * Define multiple baseUrls that will be used as a prefix for all relative urls. Assigned once per
   * virtual user based on a round-robin strategy.
   *
   * @param urls the base urls
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder baseUrls(@NonNull List<String> urls) {
    return new HttpProtocolBuilder(wrapped.baseUrls(toScalaSeq(urls)));
  }

  /**
   * Define the warmup url. Used to perform a blank HTTP request to load the classes in the
   * ClassLoader so the first load test request won't have to pay for this penalty. Hit
   * "https://gatling.io" by default.
   *
   * @param url the warmup url
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder warmUp(@NonNull String url) {
    return new HttpProtocolBuilder(wrapped.warmUp(url));
  }

  /**
   * Disable the warmup
   *
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder disableWarmUp() {
    return new HttpProtocolBuilder(wrapped.disableWarmUp());
  }

  // enginePart
  /**
   * Share a global connection pool and a global {@link javax.net.ssl.SSLContext} amongst virtual
   * users instead of each having its own. Makes sense if you don't want to generate mob browser
   * traffic but server to server traffic.
   *
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder shareConnections() {
    return new HttpProtocolBuilder(wrapped.shareConnections());
  }

  /**
   * Define a virtual host
   *
   * @param virtualHost the virtual host, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder virtualHost(@NonNull String virtualHost) {
    return new HttpProtocolBuilder(wrapped.virtualHost(toStringExpression(virtualHost)));
  }

  /**
   * Define a virtual host
   *
   * @param virtualHost the virtual host, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder virtualHost(@NonNull Function<Session, String> virtualHost) {
    return new HttpProtocolBuilder(wrapped.virtualHost(javaFunctionToExpression(virtualHost)));
  }

  /**
   * Define the local address to bind from
   *
   * @param address the local address
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder localAddress(@NonNull String address) {
    return new HttpProtocolBuilder(wrapped.localAddress(address));
  }

  /**
   * Define multiple local addresses to bind from. Assigned once per virtual user based on a
   * round-robin strategy.
   *
   * @param addresses the local addresses
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder localAddresses(@NonNull String... addresses) {
    return new HttpProtocolBuilder(wrapped.localAddresses(toScalaSeq(addresses)));
  }

  /**
   * Define multiple local addresses to bind from. Assigned once per virtual user based on a
   * round-robin strategy.
   *
   * @param addresses the local addresses
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder localAddresses(@NonNull List<String> addresses) {
    return new HttpProtocolBuilder(wrapped.localAddresses(toScalaSeq(addresses)));
  }

  /**
   * Bind from all detected local addresses. Assigned once per virtual user based on a round-robin
   * strategy.
   *
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder useAllLocalAddresses() {
    return new HttpProtocolBuilder(wrapped.useAllLocalAddresses());
  }

  /**
   * Bind from all detected local addresses matching at least one of the configured patterns.
   * Assigned once per virtual user based on a round-robin strategy.
   *
   * @param patterns some <a
   *     href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java Regular
   *     Expression</a> patterns
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder useAllLocalAddressesMatching(@NonNull String... patterns) {
    return new HttpProtocolBuilder(wrapped.useAllLocalAddressesMatching(toScalaSeq(patterns)));
  }

  /**
   * Define an HTTP/1.1 connections per host limit for fetching concurrent resources
   *
   * @param max the limit
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder maxConnectionsPerHost(int max) {
    return new HttpProtocolBuilder(wrapped.maxConnectionsPerHost(max));
  }

  /**
   * Define a function to assign a {@link KeyManagerFactory} per virtual user.
   *
   * @param f the function. Input is the virtual user's unique id
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder perUserKeyManagerFactory(
      @NonNull Function<Long, KeyManagerFactory> f) {
    return new HttpProtocolBuilder(
        wrapped.perUserKeyManagerFactory(untyped -> f.apply((Long) untyped)));
  }

  // requestPart

  /**
   * Disable the automatic Referer header generation, based on previous requests.
   *
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder disableAutoReferer() {
    return new HttpProtocolBuilder(wrapped.disableAutoReferer());
  }

  /**
   * Disable the automatic Origin header generation, based the request url.
   *
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder disableAutoOrigin() {
    return new HttpProtocolBuilder(wrapped.disableAutoOrigin());
  }

  /**
   * Disable HTTP caching.
   *
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder disableCaching() {
    return new HttpProtocolBuilder(wrapped.disableCaching());
  }

  /**
   * Set a header that's common to all HTTP requests
   *
   * @param name the static header name
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder header(@NonNull CharSequence name, @NonNull String value) {
    return new HttpProtocolBuilder(wrapped.header(name, toStringExpression(value)));
  }

  /**
   * Set a header that's common to all HTTP requests
   *
   * @param name the static header name
   * @param value the header value, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder header(
      @NonNull CharSequence name, @NonNull Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.header(name, javaFunctionToExpression(value)));
  }

  /**
   * Set multiple headers that's common to all HTTP requests
   *
   * @param headers the headers, names are static but values are expressed as a Gatling Expression
   *     Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder headers(Map<? extends CharSequence, String> headers) {
    return new HttpProtocolBuilder(wrapped.headers(toScalaMap(headers)));
  }

  /**
   * Set the accept header
   *
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder acceptHeader(@NonNull String value) {
    return new HttpProtocolBuilder(wrapped.acceptHeader(toStringExpression(value)));
  }

  /**
   * Set the accept header
   *
   * @param value the header value, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder acceptHeader(@NonNull Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.acceptHeader(javaFunctionToExpression(value)));
  }

  /**
   * Set the accept-charset header
   *
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder acceptCharsetHeader(@NonNull String value) {
    return new HttpProtocolBuilder(wrapped.acceptCharsetHeader(toStringExpression(value)));
  }

  /**
   * Set the accept-charset header
   *
   * @param value the header value, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder acceptCharsetHeader(@NonNull Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.acceptCharsetHeader(javaFunctionToExpression(value)));
  }

  /**
   * Set the accept-encoding header
   *
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder acceptEncodingHeader(@NonNull String value) {
    return new HttpProtocolBuilder(wrapped.acceptEncodingHeader(toStringExpression(value)));
  }

  /**
   * Set the accept-encoding header
   *
   * @param value the header value, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder acceptEncodingHeader(@NonNull Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.acceptEncodingHeader(javaFunctionToExpression(value)));
  }

  /**
   * Set the accept-language header
   *
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder acceptLanguageHeader(@NonNull String value) {
    return new HttpProtocolBuilder(wrapped.acceptLanguageHeader(toStringExpression(value)));
  }

  /**
   * Set the accept-language header
   *
   * @param value the header value, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder acceptLanguageHeader(@NonNull Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.acceptLanguageHeader(javaFunctionToExpression(value)));
  }

  /**
   * Set the authorization header
   *
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder authorizationHeader(@NonNull String value) {
    return new HttpProtocolBuilder(wrapped.authorizationHeader(toStringExpression(value)));
  }

  /**
   * Set the authorization header
   *
   * @param value the header value, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder authorizationHeader(@NonNull Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.authorizationHeader(javaFunctionToExpression(value)));
  }

  /**
   * Set the connection header
   *
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder connectionHeader(@NonNull String value) {
    return new HttpProtocolBuilder(wrapped.connectionHeader(toStringExpression(value)));
  }

  /**
   * Set the connection header
   *
   * @param value the header value, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder connectionHeader(@NonNull Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.connectionHeader(javaFunctionToExpression(value)));
  }

  /**
   * Set the content-type header
   *
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder contentTypeHeader(@NonNull String value) {
    return new HttpProtocolBuilder(wrapped.contentTypeHeader(toStringExpression(value)));
  }

  /**
   * Set the content-type header
   *
   * @param value the header value, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder contentTypeHeader(@NonNull Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.contentTypeHeader(javaFunctionToExpression(value)));
  }

  /**
   * Set the do-not-track header
   *
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder doNotTrackHeader(@NonNull String value) {
    return new HttpProtocolBuilder(wrapped.doNotTrackHeader(toStringExpression(value)));
  }

  /**
   * Set the do-not-track header
   *
   * @param value the header value, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder doNotTrackHeader(@NonNull Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.doNotTrackHeader(javaFunctionToExpression(value)));
  }

  /**
   * Set the origin header
   *
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder originHeader(@NonNull String value) {
    return new HttpProtocolBuilder(wrapped.originHeader(toStringExpression(value)));
  }

  /**
   * Set the origin header
   *
   * @param value the header value, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder originHeader(@NonNull Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.originHeader(javaFunctionToExpression(value)));
  }

  /**
   * Set the user-agent header
   *
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder userAgentHeader(@NonNull String value) {
    return new HttpProtocolBuilder(wrapped.userAgentHeader(toStringExpression(value)));
  }

  /**
   * Set the user-agent header
   *
   * @param value the header value, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder userAgentHeader(@NonNull Function<Session, String> value) {
    return new HttpProtocolBuilder(wrapped.userAgentHeader(javaFunctionToExpression(value)));
  }

  /**
   * Set the upgrade-insecure-requests header
   *
   * @param value the header value, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder upgradeInsecureRequestsHeader(@NonNull String value) {
    return new HttpProtocolBuilder(
        wrapped.upgradeInsecureRequestsHeader(toStringExpression(value)));
  }

  /**
   * Set the upgrade-insecure-requests header
   *
   * @param value the header value, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder upgradeInsecureRequestsHeader(
      @NonNull Function<Session, String> value) {
    return new HttpProtocolBuilder(
        wrapped.upgradeInsecureRequestsHeader(javaFunctionToExpression(value)));
  }

  /**
   * Set the authorization header for Basic Auth
   *
   * @param username the username, expressed as a Gatling Expression Language String
   * @param password the password, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder basicAuth(@NonNull String username, @NonNull String password) {
    return new HttpProtocolBuilder(
        wrapped.basicAuth(toStringExpression(username), toStringExpression(password)));
  }

  /**
   * Set the authorization header for Basic Auth
   *
   * @param username the username, expressed as a Gatling Expression Language String
   * @param password the password, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder basicAuth(
      @NonNull String username, @NonNull Function<Session, String> password) {
    return new HttpProtocolBuilder(
        wrapped.basicAuth(toStringExpression(username), javaFunctionToExpression(password)));
  }

  /**
   * Set the authorization header for Basic Auth
   *
   * @param username the username, expressed as a function
   * @param password the password, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder basicAuth(
      @NonNull Function<Session, String> username, @NonNull String password) {
    return new HttpProtocolBuilder(
        wrapped.basicAuth(javaFunctionToExpression(username), toStringExpression(password)));
  }

  /**
   * Set the authorization header for Basic Auth
   *
   * @param username the username, expressed as a function
   * @param password the password, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder basicAuth(
      @NonNull Function<Session, String> username, @NonNull Function<Session, String> password) {
    return new HttpProtocolBuilder(
        wrapped.basicAuth(javaFunctionToExpression(username), javaFunctionToExpression(password)));
  }

  /**
   * Set the authorization header for Digest Auth
   *
   * @param username the username, expressed as a Gatling Expression Language String
   * @param password the password, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder digestAuth(@NonNull String username, @NonNull String password) {
    return new HttpProtocolBuilder(
        wrapped.digestAuth(toStringExpression(username), toStringExpression(password)));
  }

  /**
   * Set the authorization header for Digest Auth
   *
   * @param username the username, expressed as a Gatling Expression Language String
   * @param password the password, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder digestAuth(
      @NonNull String username, @NonNull Function<Session, String> password) {
    return new HttpProtocolBuilder(
        wrapped.digestAuth(toStringExpression(username), javaFunctionToExpression(password)));
  }

  /**
   * Set the authorization header for Digest Auth
   *
   * @param username the username, expressed as a function
   * @param password the password, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder digestAuth(
      @NonNull Function<Session, String> username, @NonNull String password) {
    return new HttpProtocolBuilder(
        wrapped.digestAuth(javaFunctionToExpression(username), toStringExpression(password)));
  }

  /**
   * Set the authorization header for Digest Auth
   *
   * @param username the username, expressed as a function
   * @param password the password, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder digestAuth(
      @NonNull Function<Session, String> username, @NonNull Function<Session, String> password) {
    return new HttpProtocolBuilder(
        wrapped.digestAuth(javaFunctionToExpression(username), javaFunctionToExpression(password)));
  }

  /**
   * Instruct the reporting engine to not report resources
   *
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder silentResources() {
    return new HttpProtocolBuilder(wrapped.silentResources());
  }

  /**
   * Instruct the reporting engine to not report requests whose uri matches the configured <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java Regular
   * Expression</a> pattern
   *
   * @param pattern the regex pattern
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder silentUri(@NonNull String pattern) {
    return new HttpProtocolBuilder(wrapped.silentUri(pattern));
  }

  /**
   * Disable the automatic url encoding that tries to detect unescaped reserved chars
   *
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder disableUrlEncoding() {
    return new HttpProtocolBuilder(wrapped.disableUrlEncoding());
  }

  /**
   * Provide a function to sign the requests before writing them on the wire
   *
   * @param calculator the signing function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder sign(@NonNull Consumer<Request> calculator) {
    return sign((request, session) -> calculator.accept(request));
  }

  /**
   * Provide a function to sign the requests before writing them on the wire. This version provides
   * access to the session.
   *
   * @param calculator the signing function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder sign(@NonNull BiConsumer<Request, Session> calculator) {
    return new HttpProtocolBuilder(wrapped.sign(SignatureCalculators.toScala(calculator)));
  }

  /**
   * Instruct sign the requests with OAuth1 before writing them on the wire
   *
   * @param consumerKey the consumerKey, expressed as a Gatling Expression Language String
   * @param clientSharedSecret the clientSharedSecret, expressed as a Gatling Expression Language
   *     String
   * @param token the token, expressed as a Gatling Expression Language String
   * @param tokenSecret the tokenSecret, expressed as a Gatling Expression Language String
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder signWithOAuth1(
      @NonNull String consumerKey,
      @NonNull String clientSharedSecret,
      @NonNull String token,
      @NonNull String tokenSecret) {
    return new HttpProtocolBuilder(
        wrapped.signWithOAuth1(
            toStringExpression(consumerKey),
            toStringExpression(clientSharedSecret),
            toStringExpression(token),
            toStringExpression(tokenSecret)));
  }

  /**
   * Instruct sign the requests with OAuth1 before writing them on the wire
   *
   * @param consumerKey the consumerKey, expressed as a function
   * @param clientSharedSecret the clientSharedSecret, expressed as a function
   * @param token the token, expressed as a function
   * @param tokenSecret the tokenSecret, expressed as a function
   * @return a new HttpProtocolBuilder instance
   */
  @NonNull
  public HttpProtocolBuilder signWithOAuth1(
      @NonNull Function<Session, String> consumerKey,
      @NonNull Function<Session, String> clientSharedSecret,
      @NonNull Function<Session, String> token,
      @NonNull Function<Session, String> tokenSecret) {
    return new HttpProtocolBuilder(
        wrapped.signWithOAuth1(
            javaFunctionToExpression(consumerKey),
            javaFunctionToExpression(clientSharedSecret),
            javaFunctionToExpression(token),
            javaFunctionToExpression(tokenSecret)));
  }

  /**
   * Enable HTTP/2
   *
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder enableHttp2() {
    return new HttpProtocolBuilder(wrapped.enableHttp2());
  }

  /**
   * Define the remote hosts that are known to support or not support HTTP/2
   *
   * @param remotes the known remote hosts
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder http2PriorKnowledge(Map<String, Boolean> remotes) {
    return new HttpProtocolBuilder(
        wrapped.http2PriorKnowledge(
            toScalaMap(
                remotes.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))));
  }

  // responsePart
  /**
   * Disable automatically following redirects
   *
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder disableFollowRedirect() {
    return new HttpProtocolBuilder(wrapped.disableFollowRedirect());
  }

  /**
   * Define the maximum number of redirects in a redirect chain
   *
   * @param max the limit
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder maxRedirects(int max) {
    return new HttpProtocolBuilder(wrapped.maxRedirects(max));
  }

  /**
   * Apply 302 strictly and not switch to GET and re-send the request body
   *
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder strict302Handling() {
    return new HttpProtocolBuilder(wrapped.strict302Handling());
  }

  /** A function for providing a naming strategy for requests generated from a redirect */
  @FunctionalInterface
  public interface RedirectNamingStrategy {
    /**
     * Generate a name for a redirected request
     *
     * @param uri the location uro
     * @param originalRequestName the original request name
     * @param redirectCount the current number of consecutive redirects in the chain
     * @return a name for the request
     */
    String apply(Uri uri, String originalRequestName, int redirectCount);
  }

  /**
   * Define a naming strategy for requests generated from a redirect
   *
   * @param f the strategy
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder redirectNamingStrategy(RedirectNamingStrategy f) {
    return new HttpProtocolBuilder(
        wrapped.redirectNamingStrategy(
            (uri, originalRequestName, redirectCount) ->
                f.apply(uri, originalRequestName, ((Integer) redirectCount))));
  }

  /**
   * Define a transformation function to be applied on the {@link Response}s before checks are
   * applied. Typically used for decoding responses, eg with <a
   * href="https://developers.google.com/protocol-buffers">Protobuf</a>.
   *
   * @param f the strategy
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder transformResponse(BiFunction<Response, Session, Response> f) {
    return new HttpProtocolBuilder(wrapped.transformResponse(javaBiFunctionToExpression(f)));
  }

  /**
   * Define some common checks to be applied on all the requests.
   *
   * @param checks the checks
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder check(CheckBuilder... checks) {
    return check(Arrays.asList(checks));
  }

  /**
   * Define some common checks to be applied on all the requests.
   *
   * @param checks the checks
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder check(List<CheckBuilder> checks) {
    return new HttpProtocolBuilder(wrapped.check(toScalaChecks(checks)));
  }

  /**
   * Define some common checks to be applied on all the requests when a condition holds true.
   *
   * @param condition a condition, expressed as a function
   * @return the next DSL step
   */
  public UntypedCondition checkIf(Function<Session, Boolean> condition) {
    return new UntypedCondition(ScalaHttpProtocolBuilderConditions.untyped(wrapped, condition));
  }

  /**
   * Define some common checks to be applied on all the requests when a condition holds true.
   *
   * @param condition a condition, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  public UntypedCondition checkIf(String condition) {
    return new UntypedCondition(ScalaHttpProtocolBuilderConditions.untyped(wrapped, condition));
  }

  public static final class UntypedCondition {
    private final ScalaHttpProtocolBuilderConditions.Untyped wrapped;

    public UntypedCondition(ScalaHttpProtocolBuilderConditions.Untyped wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the checks to apply when the condition holds true.
     *
     * @param thenChecks the checks
     * @return a new HttpProtocolBuilder instance
     */
    public HttpProtocolBuilder then(CheckBuilder... thenChecks) {
      return then(Arrays.asList(thenChecks));
    }

    /**
     * Define the checks to apply when the condition holds true.
     *
     * @param thenChecks the checks
     * @return a new HttpProtocolBuilder instance
     */
    public HttpProtocolBuilder then(List<CheckBuilder> thenChecks) {
      return wrapped.then_(thenChecks);
    }
  }

  /**
   * Define some common checks to be applied on all the requests when a condition holds true.
   *
   * @param condition a condition, expressed as a function that's aware of the HTTP response and the
   *     Session
   * @return the next DSL step
   */
  public TypedCondition checkIf(BiFunction<Response, Session, Boolean> condition) {
    return new TypedCondition(ScalaHttpProtocolBuilderConditions.typed(wrapped, condition));
  }

  public static final class TypedCondition {
    private final ScalaHttpProtocolBuilderConditions.Typed wrapped;

    public TypedCondition(ScalaHttpProtocolBuilderConditions.Typed wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the checks to apply when the condition holds true.
     *
     * @param thenChecks the checks
     * @return a new HttpProtocolBuilder instance
     */
    public HttpProtocolBuilder then(CheckBuilder... thenChecks) {
      return then(Arrays.asList(thenChecks));
    }

    /**
     * Define the checks to apply when the condition holds true.
     *
     * @param thenChecks the checks
     * @return a new HttpProtocolBuilder instance
     */
    public HttpProtocolBuilder then(List<CheckBuilder> thenChecks) {
      return wrapped.then_(thenChecks);
    }
  }

  /**
   * Automatically infer resources from HTML payloads
   *
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder inferHtmlResources() {
    return new HttpProtocolBuilder(wrapped.inferHtmlResources());
  }

  /**
   * Automatically infer resources from HTML payloads
   *
   * @param allow the allow list to filter the resources
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder inferHtmlResources(Filter.AllowList allow) {
    return new HttpProtocolBuilder(wrapped.inferHtmlResources(allow.asScala()));
  }

  /**
   * Automatically infer resources from HTML payloads
   *
   * @param allow the allow list to filter the resources
   * @param deny the deny list to filter out the resources
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder inferHtmlResources(Filter.AllowList allow, Filter.DenyList deny) {
    return new HttpProtocolBuilder(wrapped.inferHtmlResources(allow.asScala(), deny.asScala()));
  }

  /**
   * Automatically infer resources from HTML payloads
   *
   * @param deny the deny list to filter out the resources
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder inferHtmlResources(Filter.DenyList deny) {
    return new HttpProtocolBuilder(wrapped.inferHtmlResources(deny.asScala()));
  }

  /**
   * Name the inferred resources' requests based on the tail of the url
   *
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder nameInferredHtmlResourcesAfterUrlTail() {
    return new HttpProtocolBuilder(wrapped.nameInferredHtmlResourcesAfterUrlTail());
  }

  /**
   * Name the inferred resources' requests based on the absolute url
   *
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder nameInferredHtmlResourcesAfterAbsoluteUrl() {
    return new HttpProtocolBuilder(wrapped.nameInferredHtmlResourcesAfterAbsoluteUrl());
  }

  /**
   * Name the inferred resources' requests based on the relative url
   *
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder nameInferredHtmlResourcesAfterRelativeUrl() {
    return new HttpProtocolBuilder(wrapped.nameInferredHtmlResourcesAfterRelativeUrl());
  }

  /**
   * Name the inferred resources' requests based on the path
   *
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder nameInferredHtmlResourcesAfterPath() {
    return new HttpProtocolBuilder(wrapped.nameInferredHtmlResourcesAfterPath());
  }

  /**
   * Name the inferred resources' requests based on the last element of the path
   *
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder nameInferredHtmlResourcesAfterLastPathElement() {
    return new HttpProtocolBuilder(wrapped.nameInferredHtmlResourcesAfterLastPathElement());
  }

  /**
   * Name the inferred resources' requests based on the provided function
   *
   * @param f the naming function, aware of the full uri
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder nameInferredHtmlResources(Function<Uri, String> f) {
    return new HttpProtocolBuilder(wrapped.nameInferredHtmlResources(f::apply));
  }

  // wsPart
  /**
   * Define a baseUrl that will be used as a prefix for all relative WebSocket urls.
   *
   * @param url the base url
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder wsBaseUrl(String url) {
    return new HttpProtocolBuilder(wrapped.wsBaseUrl(url));
  }

  /**
   * Define multiple baseUrls that will be used as a prefix for all relative WebSocket urls.
   * Assigned once per virtual user based on a round-robin strategy.
   *
   * @param urls the base urls
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder wsBaseUrls(String... urls) {
    return new HttpProtocolBuilder(wrapped.wsBaseUrls(toScalaSeq(urls)));
  }

  /**
   * Define multiple baseUrls that will be used as a prefix for all relative WebSocket urls.
   * Assigned once per virtual user based on a round-robin strategy.
   *
   * @param urls the base urls
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder wsBaseUrls(List<String> urls) {
    return new HttpProtocolBuilder(wrapped.wsBaseUrls(toScalaSeq(urls)));
  }

  /**
   * Automatically reconnect disconnected WebSockets
   *
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder wsReconnect() {
    return new HttpProtocolBuilder(wrapped.wsReconnect());
  }

  /**
   * Define a maximum number of times a WebSocket can be automatically reconnected
   *
   * @param max the limit
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder wsMaxReconnects(int max) {
    return new HttpProtocolBuilder(wrapped.wsMaxReconnects(max));
  }

  /**
   * Automatically reply to a TEXT frame with another TEXT frame.
   *
   * @param f the function
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder wsAutoReplyTextFrame(Function<String, String> f) {
    return new HttpProtocolBuilder(
        wrapped.wsAutoReplyTextFrame(
            new PartialFunction<String, String>() {
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

  /**
   * Automatically reply to a SocketIo4 ping TEXT frame with the corresponding pong TEXT frame.
   *
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder wsAutoReplySocketIo4() {
    return new HttpProtocolBuilder(wrapped.wsAutoReplySocketIo4());
  }

  // proxyPart
  /**
   * Ignore any configured proxy for some hosts
   *
   * @param hosts the hosts that must be connected directly without the proxy
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder noProxyFor(String... hosts) {
    return new HttpProtocolBuilder(wrapped.noProxyFor(toScalaSeq(hosts)));
  }

  /**
   * Define a Proxy to be used for all requests
   *
   * @param proxy the proxy
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder proxy(Proxy proxy) {
    return new HttpProtocolBuilder(wrapped.proxy(proxy.asScala()));
  }

  // dnsPart
  /**
   * Enable Gatling non-blocking DNS resolution instead of using Java's blocking implementation
   *
   * @param dnsServers the DNS servers
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder asyncNameResolution(String... dnsServers) {
    return new HttpProtocolBuilder(wrapped.asyncNameResolution(toScalaSeq(dnsServers)));
  }

  /**
   * Enable Gatling non-blocking DNS resolution instead of using Java's blocking implementation
   *
   * @param dnsServers the DNS servers
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder asyncNameResolution(InetSocketAddress[] dnsServers) {
    return new HttpProtocolBuilder(wrapped.asyncNameResolution(dnsServers));
  }

  /**
   * Define some aliases to bypass DNS name resolution
   *
   * @param aliases the aliases
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder hostNameAliases(Map<String, List<String>> aliases) {
    return new HttpProtocolBuilder(
        wrapped.hostNameAliases(HttpProtocolBuilders.toScalaAliases(aliases)));
  }

  /**
   * Force each virtual user to have its own DNS cache and perform its own DNS resolutions instead
   * of using a global shared resolver
   *
   * @return a new HttpProtocolBuilder instance
   */
  public HttpProtocolBuilder perUserNameResolution() {
    return new HttpProtocolBuilder(wrapped.perUserNameResolution());
  }
}
