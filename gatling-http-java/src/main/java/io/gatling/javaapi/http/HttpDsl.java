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

package io.gatling.javaapi.http;

import static io.gatling.javaapi.core.internal.Expressions.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.commons.validation.Validation;
import io.gatling.core.action.builder.SessionHookBuilder;
import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.internal.HttpCheckBuilder;
import io.gatling.javaapi.http.internal.HttpCheckBuilders;
import io.gatling.javaapi.http.internal.HttpCheckType;
import java.util.function.Function;
import scala.Function1;

/** The entrypoint of the Gatling HTTP DSL */
public final class HttpDsl {
  private HttpDsl() {}

  ////////// HttpDsl

  /** Bootstrap a HTTP protocol configuration */
  public static final HttpProtocolBuilder http =
      new HttpProtocolBuilder(
          io.gatling.http.protocol.HttpProtocolBuilder.apply(
              io.gatling.core.Predef.configuration()));

  /**
   * Bootstrap a HTTP request configuration
   *
   * @param name the HTTP request name, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static Http http(@NonNull String name) {
    return new Http(toStringExpression(name));
  }

  /**
   * Bootstrap a HTTP request configuration
   *
   * @param name the HTTP request name, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static Http http(@NonNull Function<Session, String> name) {
    return new Http(javaFunctionToExpression(name));
  }

  /**
   * Bootstrap a WebSocket request configuration
   *
   * @param name the WebSocket request name, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static Ws ws(@NonNull String name) {
    return new Ws(io.gatling.http.action.ws.Ws.apply(toStringExpression(name)));
  }

  /**
   * Bootstrap a WebSocket request configuration
   *
   * @param name the WebSocket request name, expressed as a Gatling Expression Language String
   * @param wsName the name of the WebSocket so multiple WebSockets for the same virtual users don't
   *     conflict, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static Ws ws(@NonNull String name, @NonNull String wsName) {
    return new Ws(
        io.gatling.http.action.ws.Ws.apply(toStringExpression(name), toStringExpression(wsName)));
  }

  public static final Ws.Prefix ws = Ws.Prefix.INSTANCE;

  /**
   * Bootstrap a <a
   * href="https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events">SSE</a> request
   * configuration
   *
   * @param name the SSE request name, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static Sse sse(@NonNull String name) {
    return new Sse(io.gatling.http.action.sse.Sse.apply(toStringExpression(name)));
  }

  /**
   * Bootstrap a <a
   * href="https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events">SSE</a> request
   * configuration
   *
   * @param name the SSE request name, expressed as a Gatling Expression Language String
   * @param sseName the name of the SSE stream so multiple SSE streams for the same virtual users
   *     don't conflict, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static Sse sse(@NonNull String name, @NonNull String sseName) {
    return new Sse(
        io.gatling.http.action.sse.Sse.apply(
            toStringExpression(name), toStringExpression(sseName)));
  }

  /** The prefix to bootstrap SSE specific DSL */
  public static final Sse.Prefix sse = Sse.Prefix.INSTANCE;

  /** The prefix to bootstrap polling specific DSL */
  public static Polling poll() {
    return Polling.DEFAULT;
  }

  ////////// HttpCheckSupport

  /**
   * Bootstrap a check that capture the response HTTP status code
   *
   * @return the next step in the check DSL
   */
  @NonNull
  public static CheckBuilder.Find<Integer> status() {
    return HttpCheckBuilders.status();
  }

  /**
   * Bootstrap a check that capture the response location, eg the landing url in a chain of
   * redirects
   *
   * @return the next step in the check DSL
   */
  @NonNull
  public static CheckBuilder.Find<String> currentLocation() {
    return new CheckBuilder.Find.Default<>(
        io.gatling.http.Predef.currentLocation(),
        HttpCheckType.CurrentLocation,
        String.class,
        null);
  }

  /**
   * Bootstrap a check that capture some <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java Regular
   * Expression</a> capture groups on the response location, eg the landing url in a chain of
   * redirects
   *
   * @param pattern the regular expression, expressed as a Gatling Expression Language String
   * @return the next step in the check DSL
   */
  @NonNull
  public static CheckBuilder.CaptureGroupCheckBuilder currentLocationRegex(
      @NonNull String pattern) {
    return new HttpCheckBuilder.CurrentLocationRegex(
        io.gatling.http.Predef.currentLocationRegex(
            toStringExpression(pattern), io.gatling.core.Predef.defaultPatterns()));
  }

  /**
   * Bootstrap a check that capture some <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java Regular
   * Expression</a> capture groups on the response location, eg the landing url in a chain of
   * redirects
   *
   * @param pattern the regular expression, expressed as a function
   * @return the next step in the check DSL
   */
  @NonNull
  public static HttpCheckBuilder.CurrentLocationRegex currentLocationRegex(
      @NonNull Function<Session, String> pattern) {
    return new HttpCheckBuilder.CurrentLocationRegex(
        io.gatling.http.Predef.currentLocationRegex(
            javaFunctionToExpression(pattern), io.gatling.core.Predef.defaultPatterns()));
  }

  /**
   * Bootstrap a check that capture the value of a HTTP header
   *
   * @param name the static name of the HTTP header
   * @return the next step in the check DSL
   */
  @NonNull
  public static CheckBuilder.MultipleFind<String> header(@NonNull CharSequence name) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.http.Predef.header(toStaticValueExpression(name)),
        HttpCheckType.Header,
        String.class,
        null);
  }

  /**
   * Bootstrap a check that capture the value of a HTTP header
   *
   * @param name the name of the HTTP header, expressed as a Gatling Expression Language String
   * @return the next step in the check DSL
   */
  @NonNull
  public static CheckBuilder.MultipleFind<String> header(@NonNull String name) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.http.Predef.header(toExpression(name, CharSequence.class)),
        HttpCheckType.Header,
        String.class,
        null);
  }

  /**
   * Bootstrap a check that capture the value of a HTTP header
   *
   * @param name the name of the HTTP header, expressed as a function
   * @return the next step in the check DSL
   */
  @NonNull
  public static CheckBuilder.MultipleFind<String> header(
      @NonNull Function<Session, CharSequence> name) {
    return new CheckBuilder.MultipleFind.Default<>(
        io.gatling.http.Predef.header(javaFunctionToExpression(name)),
        HttpCheckType.Header,
        String.class,
        null);
  }

  /**
   * Bootstrap a check that capture some <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java Regular
   * Expression</a> capture groups on a response header
   *
   * @param name the static name of the HTTP header
   * @param pattern the regular expression, expressed as a Gatling Expression Language String
   * @return the next step in the check DSL
   */
  @NonNull
  public static CheckBuilder.CaptureGroupCheckBuilder headerRegex(
      CharSequence name, String pattern) {
    return new HttpCheckBuilder.HeaderRegexCheck(
        io.gatling.http.Predef.headerRegex(
            toStaticValueExpression(name),
            toStringExpression(pattern),
            io.gatling.core.Predef.defaultPatterns()));
  }

  /**
   * Bootstrap a check that capture some <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java Regular
   * Expression</a> capture groups on a response header
   *
   * @param name the name of the HTTP header, expressed as a Gatling Expression Language String
   * @param pattern the regular expression, expressed as a Gatling Expression Language String
   * @return the next step in the check DSL
   */
  @NonNull
  public static CheckBuilder.CaptureGroupCheckBuilder headerRegex(
      @NonNull String name, @NonNull String pattern) {
    return new HttpCheckBuilder.HeaderRegexCheck(
        io.gatling.http.Predef.headerRegex(
            toExpression(name, CharSequence.class),
            toStringExpression(pattern),
            io.gatling.core.Predef.defaultPatterns()));
  }

  /**
   * Bootstrap a check that capture some <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java Regular
   * Expression</a> capture groups on a response header
   *
   * @param name the name of the HTTP header, expressed as a function
   * @param pattern the regular expression, expressed as a Gatling Expression Language String
   * @return the next step in the check DSL
   */
  @NonNull
  public static CheckBuilder.CaptureGroupCheckBuilder headerRegex(
      @NonNull Function<Session, CharSequence> name, @NonNull String pattern) {
    return new HttpCheckBuilder.HeaderRegexCheck(
        io.gatling.http.Predef.headerRegex(
            javaFunctionToExpression(name),
            toStringExpression(pattern),
            io.gatling.core.Predef.defaultPatterns()));
  }

  /**
   * Bootstrap a check that capture some <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java Regular
   * Expression</a> capture groups on a response header
   *
   * @param name the static name of the HTTP header
   * @param pattern the regular expression, expressed as a function
   * @return the next step in the check DSL
   */
  @NonNull
  public static CheckBuilder.CaptureGroupCheckBuilder headerRegex(
      @NonNull CharSequence name, @NonNull Function<Session, String> pattern) {
    return new HttpCheckBuilder.HeaderRegexCheck(
        io.gatling.http.Predef.headerRegex(
            toStaticValueExpression(name),
            javaFunctionToExpression(pattern),
            io.gatling.core.Predef.defaultPatterns()));
  }

  /**
   * Bootstrap a check that capture some <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java Regular
   * Expression</a> capture groups on a response header
   *
   * @param name the name of the HTTP header, expressed as a Gatling Expression Language String
   * @param pattern the regular expression, expressed as a function
   * @return the next step in the check DSL
   */
  @NonNull
  public static CheckBuilder.CaptureGroupCheckBuilder headerRegex(
      @NonNull String name, @NonNull Function<Session, String> pattern) {
    return new HttpCheckBuilder.HeaderRegexCheck(
        io.gatling.http.Predef.headerRegex(
            toExpression(name, CharSequence.class),
            javaFunctionToExpression(pattern),
            io.gatling.core.Predef.defaultPatterns()));
  }

  /**
   * Bootstrap a check that capture some <a
   * href="https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html">Java Regular
   * Expression</a> capture groups on a response header
   *
   * @param name the name of the HTTP header, expressed as a function
   * @param pattern the regular expression, expressed as a function
   * @return the next step in the check DSL
   */
  @NonNull
  public static CheckBuilder.CaptureGroupCheckBuilder headerRegex(
      @NonNull Function<Session, CharSequence> name, @NonNull Function<Session, String> pattern) {
    return new HttpCheckBuilder.HeaderRegexCheck(
        io.gatling.http.Predef.headerRegex(
            javaFunctionToExpression(name),
            javaFunctionToExpression(pattern),
            io.gatling.core.Predef.defaultPatterns()));
  }

  ////////// SitemapFeederSupport

  /**
   * Bootstrap a feeder that reads from a sitemap XML file
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static FeederBuilder.FileBased<String> sitemap(@NonNull String filePath) {
    return new FeederBuilder.Impl<>(
        io.gatling.http.Predef.sitemap(filePath, io.gatling.core.Predef.configuration()));
  }

  ////////// BodyPartSupport
  /**
   * Bootstrap a {@link BodyPart} backed by a file whose text context will be interpreted as a
   * Gatling Expression Language String. The name of the part is equal to the file name.
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart ElFileBodyPart(@NonNull String filePath) {
    return ElFileBodyPart(toStringExpression(filePath));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file whose text context will be interpreted as a
   * Gatling Expression Language String The name of the part is equal to the file name.
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart ElFileBodyPart(@NonNull Function<Session, String> filePath) {
    return ElFileBodyPart(javaFunctionToExpression(filePath));
  }

  private static BodyPart ElFileBodyPart(
      @NonNull Function1<io.gatling.core.session.Session, Validation<String>> filePath) {
    return new BodyPart(
        io.gatling.http.Predef.ElFileBodyPart(
            filePath,
            io.gatling.core.Predef.configuration(),
            io.gatling.core.Predef.elFileBodies()));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file whose text context will be interpreted as a
   * Gatling Expression Language String.
   *
   * @param name the name of the part, expressed as a Gatling Expression Language String
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart ElFileBodyPart(@NonNull String name, @NonNull String filePath) {
    return ElFileBodyPart(toStringExpression(name), toStringExpression(filePath));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file whose text context will be interpreted as a
   * Gatling Expression Language String.
   *
   * @param name the name of the part, expressed as a Gatling Expression Language String
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart ElFileBodyPart(
      @NonNull String name, @NonNull Function<Session, String> filePath) {
    return ElFileBodyPart(toStringExpression(name), javaFunctionToExpression(filePath));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file whose text context will be interpreted as a
   * Gatling Expression Language String.
   *
   * @param name the name of the part, expressed as a function
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart ElFileBodyPart(
      @NonNull Function<Session, String> name, @NonNull String filePath) {
    return ElFileBodyPart(javaFunctionToExpression(name), toStringExpression(filePath));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file whose text context will be interpreted as a
   * Gatling Expression Language String.
   *
   * @param name the name of the part, expressed as a function
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart ElFileBodyPart(
      @NonNull Function<Session, String> name, @NonNull Function<Session, String> filePath) {
    return ElFileBodyPart(javaFunctionToExpression(name), javaFunctionToExpression(filePath));
  }

  private static BodyPart ElFileBodyPart(
      @NonNull Function1<io.gatling.core.session.Session, Validation<String>> name,
      @NonNull Function1<io.gatling.core.session.Session, Validation<String>> filePath) {
    return new BodyPart(
        io.gatling.http.Predef.ElFileBodyPart(
            name,
            filePath,
            io.gatling.core.Predef.configuration(),
            io.gatling.core.Predef.elFileBodies()));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a String
   *
   * @param string the string, interpreted as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart StringBodyPart(@NonNull String string) {
    return new BodyPart(
        io.gatling.http.Predef.StringBodyPart(
            toStringExpression(string), io.gatling.core.Predef.configuration()));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a String
   *
   * @param string the string, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart StringBodyPart(@NonNull Function<Session, String> string) {
    return ElFileBodyPart(javaFunctionToExpression(string));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a String
   *
   * @param name the name of the part, expressed as a Gatling Expression Language String
   * @param string the string, interpreted as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart StringBodyPart(@NonNull String name, @NonNull String string) {
    return StringBodyPart(toStringExpression(name), toStringExpression(string));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a String
   *
   * @param name the name of the part, expressed as a Gatling Expression Language String
   * @param string the string, interpreted as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart StringBodyPart(
      @NonNull String name, @NonNull Function<Session, String> string) {
    return StringBodyPart(toStringExpression(name), javaFunctionToExpression(string));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a String
   *
   * @param name the name of the part, expressed as a function
   * @param string the string, interpreted as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart StringBodyPart(
      @NonNull Function<Session, String> name, @NonNull String string) {
    return StringBodyPart(javaFunctionToExpression(name), toStringExpression(string));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a String
   *
   * @param name the name of the part, expressed as a function
   * @param string the string, interpreted as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart StringBodyPart(
      @NonNull Function<Session, String> name, @NonNull Function<Session, String> string) {
    return StringBodyPart(javaFunctionToExpression(name), javaFunctionToExpression(string));
  }

  private static BodyPart StringBodyPart(
      Function1<io.gatling.core.session.Session, Validation<String>> name,
      Function1<io.gatling.core.session.Session, Validation<String>> string) {
    return new BodyPart(
        io.gatling.http.Predef.StringBodyPart(
            name, string, io.gatling.core.Predef.configuration()));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file, whose bytes will be sent as is. The name of the
   * part is equal to the name of the file.
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart RawFileBodyPart(@NonNull String filePath) {
    return RawFileBodyPart(toStringExpression(filePath));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file, whose bytes will be sent as is. The name of the
   * part is equal to the name of the file.
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart RawFileBodyPart(@NonNull Function<Session, String> filePath) {
    return RawFileBodyPart(javaFunctionToExpression(filePath));
  }

  private static BodyPart RawFileBodyPart(
      Function1<io.gatling.core.session.Session, Validation<String>> filePath) {
    return new BodyPart(
        io.gatling.http.Predef.RawFileBodyPart(filePath, io.gatling.core.Predef.rawFileBodies()));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file, whose bytes will be sent as is.
   *
   * @param name the name of the part, expressed as a Gatling Expression Language String
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart RawFileBodyPart(@NonNull String name, @NonNull String filePath) {
    return RawFileBodyPart(toStringExpression(name), toStringExpression(filePath));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file, whose bytes will be sent as is.
   *
   * @param name the name of the part, expressed as a Gatling Expression Language String
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart RawFileBodyPart(
      @NonNull String name, @NonNull Function<Session, String> filePath) {
    return RawFileBodyPart(toStringExpression(name), javaFunctionToExpression(filePath));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file, whose bytes will be sent as is.
   *
   * @param name the name of the part, expressed as a function
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart RawFileBodyPart(
      @NonNull Function<Session, String> name, @NonNull String filePath) {
    return RawFileBodyPart(javaFunctionToExpression(name), toStringExpression(filePath));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file, whose bytes will be sent as is.
   *
   * @param name the name of the part, expressed as a function
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart RawFileBodyPart(
      @NonNull Function<Session, String> name, @NonNull Function<Session, String> filePath) {
    return RawFileBodyPart(javaFunctionToExpression(name), javaFunctionToExpression(filePath));
  }

  private static BodyPart RawFileBodyPart(
      Function1<io.gatling.core.session.Session, Validation<String>> name,
      Function1<io.gatling.core.session.Session, Validation<String>> filePath) {
    return new BodyPart(
        io.gatling.http.Predef.RawFileBodyPart(
            name, filePath, io.gatling.core.Predef.rawFileBodies()));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file, whose content is interpreted as a <a
   * href="https://pebbletemplates.io/">Pebble</a> template. The name of the part is equal to the
   * name of the file.
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart PebbleFileBodyPart(@NonNull String filePath) {
    return PebbleFileBodyPart(toStringExpression(filePath));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file, whose content is interpreted as a <a
   * href="https://pebbletemplates.io/">Pebble</a> template. The name of the part is equal to the
   * name of the file.
   *
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart PebbleFileBodyPart(@NonNull Function<Session, String> filePath) {
    return PebbleFileBodyPart(javaFunctionToExpression(filePath));
  }

  private static BodyPart PebbleFileBodyPart(
      Function1<io.gatling.core.session.Session, Validation<String>> filePath) {
    return new BodyPart(
        io.gatling.http.Predef.PebbleFileBodyPart(
            filePath,
            io.gatling.core.Predef.configuration(),
            io.gatling.core.Predef.pebbleFileBodies()));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file, whose content is interpreted as a <a
   * href="https://pebbletemplates.io/">Pebble</a> template.
   *
   * @param name the name of the part, expressed as a Gatling Expression Language String
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart PebbleFileBodyPart(@NonNull String name, @NonNull String filePath) {
    return PebbleFileBodyPart(toStringExpression(name), toStringExpression(filePath));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file, whose content is interpreted as a <a
   * href="https://pebbletemplates.io/">Pebble</a> template.
   *
   * @param name the name of the part, expressed as a Gatling Expression Language String
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart PebbleFileBodyPart(
      @NonNull String name, @NonNull Function<Session, String> filePath) {
    return PebbleFileBodyPart(toStringExpression(name), javaFunctionToExpression(filePath));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file, whose content is interpreted as a <a
   * href="https://pebbletemplates.io/">Pebble</a> template.
   *
   * @param name the name of the part, expressed as a function
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart PebbleFileBodyPart(
      @NonNull Function<Session, String> name, @NonNull String filePath) {
    return PebbleFileBodyPart(javaFunctionToExpression(name), toStringExpression(filePath));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a file, whose content is interpreted as a <a
   * href="https://pebbletemplates.io/">Pebble</a> template.
   *
   * @param name the name of the part, expressed as a function
   * @param filePath the path of the file, either relative to the root of the classpath, or
   *     absolute, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart PebbleFileBodyPart(
      @NonNull Function<Session, String> name, @NonNull Function<Session, String> filePath) {
    return PebbleFileBodyPart(javaFunctionToExpression(name), javaFunctionToExpression(filePath));
  }

  private static BodyPart PebbleFileBodyPart(
      Function1<io.gatling.core.session.Session, Validation<String>> name,
      Function1<io.gatling.core.session.Session, Validation<String>> filePath) {
    return new BodyPart(
        io.gatling.http.Predef.PebbleFileBodyPart(
            name,
            filePath,
            io.gatling.core.Predef.configuration(),
            io.gatling.core.Predef.pebbleFileBodies()));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a String, whose content is interpreted as a <a
   * href="https://pebbletemplates.io/">Pebble</a> template.
   *
   * @param string the Pebble String template
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart PebbleStringBodyPart(@NonNull String string) {
    return new BodyPart(
        io.gatling.http.Predef.PebbleStringBodyPart(
            string, io.gatling.core.Predef.configuration()));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a String, whose content is interpreted as a <a
   * href="https://pebbletemplates.io/">Pebble</a> template.
   *
   * @param name the name of the part, expressed as a Gatling Expression Language String
   * @param string the Pebble String template
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart PebbleStringBodyPart(@NonNull String name, @NonNull String string) {
    return PebbleStringBodyPart(toStringExpression(name), string);
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a String, whose content is interpreted as a <a
   * href="https://pebbletemplates.io/">Pebble</a> template.
   *
   * @param name the name of the part, expressed as a function
   * @param string the Pebble String template
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart PebbleStringBodyPart(
      @NonNull Function<Session, String> name, @NonNull String string) {
    return PebbleStringBodyPart(javaFunctionToExpression(name), string);
  }

  private static BodyPart PebbleStringBodyPart(
      Function1<io.gatling.core.session.Session, Validation<String>> name, String string) {
    return new BodyPart(
        io.gatling.http.Predef.PebbleStringBodyPart(
            name, string, io.gatling.core.Predef.configuration()));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a byte array. Bytes are sent as is.
   *
   * @param name the name of the part, expressed as a Gatling Expression Language String
   * @param bytes the static bytes
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart ByteArrayBodyPart(@NonNull String name, @NonNull byte[] bytes) {
    return ByteArrayBodyPart(toStringExpression(name), toStaticValueExpression(bytes));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a byte array. Bytes are sent as is.
   *
   * @param name the name of the part, expressed as a function
   * @param bytes the static bytes
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart ByteArrayBodyPart(
      @NonNull Function<Session, String> name, @NonNull byte[] bytes) {
    return ByteArrayBodyPart(javaFunctionToExpression(name), toStaticValueExpression(bytes));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a byte array. Bytes are sent as is.
   *
   * @param name the name of the part, expressed as a Gatling Expression Language String
   * @param bytes the bytes, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart ByteArrayBodyPart(@NonNull String name, @NonNull String bytes) {
    return ByteArrayBodyPart(toStringExpression(name), toBytesExpression(bytes));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a byte array. Bytes are sent as is.
   *
   * @param name the name of the part, expressed as a function
   * @param bytes the bytes, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart ByteArrayBodyPart(
      @NonNull Function<Session, String> name, @NonNull String bytes) {
    return ByteArrayBodyPart(javaFunctionToExpression(name), toBytesExpression(bytes));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a byte array. Bytes are sent as is.
   *
   * @param name the name of the part, expressed as a Gatling Expression Language String
   * @param bytes the bytes, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart ByteArrayBodyPart(
      @NonNull String name, @NonNull Function<Session, byte[]> bytes) {
    return ByteArrayBodyPart(toStringExpression(name), javaFunctionToExpression(bytes));
  }

  /**
   * Bootstrap a {@link BodyPart} backed by a byte array. Bytes are sent as is.
   *
   * @param name the name of the part, expressed as a function
   * @param bytes the bytes, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static BodyPart ByteArrayBodyPart(
      @NonNull Function<Session, String> name, @NonNull Function<Session, byte[]> bytes) {
    return ByteArrayBodyPart(javaFunctionToExpression(name), javaFunctionToExpression(bytes));
  }

  private static BodyPart ByteArrayBodyPart(
      Function1<io.gatling.core.session.Session, Validation<String>> name,
      Function1<io.gatling.core.session.Session, Validation<byte[]>> bytes) {
    return new BodyPart(io.gatling.http.Predef.ByteArrayBodyPart(name, bytes));
  }

  ////////// CookieSupport
  /**
   * Create an action to add a Cookie
   *
   * @param cookie the DSL for adding a cookie
   * @return an ActionBuilder
   */
  @NonNull
  public static ActionBuilder addCookie(@NonNull AddCookie cookie) {
    return () -> io.gatling.http.action.cookie.AddCookieBuilder.apply(cookie.asScala());
  }

  /**
   * Create an action to get a Cookie value into the Session
   *
   * @param cookie the DSL for getting a cookie
   * @return an ActionBuilder
   */
  @NonNull
  public static ActionBuilder getCookieValue(@NonNull GetCookie cookie) {
    return () -> io.gatling.http.action.cookie.GetCookieBuilder.apply(cookie.asScala());
  }

  /**
   * Create an action to flush the Session (non-persistent) Cookies of the user
   *
   * @return an ActionBuilder
   */
  @NonNull
  public static ActionBuilder flushSessionCookies() {
    return () -> new SessionHookBuilder(io.gatling.http.Predef.flushSessionCookies(), true);
  }

  /**
   * Create an action to flush all the Cookies of the user
   *
   * @return an ActionBuilder
   */
  @NonNull
  public static ActionBuilder flushCookieJar() {
    return () -> new SessionHookBuilder(io.gatling.http.Predef.flushCookieJar(), true);
  }

  /**
   * Create an action to flush the HTTP cache of the user
   *
   * @return an ActionBuilder
   */
  @NonNull
  public static ActionBuilder flushHttpCache() {
    return () -> new SessionHookBuilder(io.gatling.http.Predef.flushHttpCache(), true);
  }

  /**
   * Bootstrap the DSL for defining a Cookie to add
   *
   * @param name the name of the cookie, expressed as a Gatling Expression Language String
   * @param value the value of the cookie, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static AddCookie Cookie(@NonNull String name, @NonNull String value) {
    return new AddCookie(
        io.gatling.http.Predef.Cookie(toStringExpression(name), toStringExpression(value)));
  }

  /**
   * Bootstrap the DSL for defining a Cookie to add
   *
   * @param name the name of the cookie, expressed as a function
   * @param value the value of the cookie, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static AddCookie Cookie(@NonNull Function<Session, String> name, @NonNull String value) {
    return new AddCookie(
        io.gatling.http.Predef.Cookie(javaFunctionToExpression(name), toStringExpression(value)));
  }

  /**
   * Bootstrap the DSL for defining a Cookie to add
   *
   * @param name the name of the cookie, expressed as a Gatling Expression Language String
   * @param value the value of the cookie, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static AddCookie Cookie(@NonNull String name, @NonNull Function<Session, String> value) {
    return new AddCookie(
        io.gatling.http.Predef.Cookie(toStringExpression(name), javaFunctionToExpression(value)));
  }

  /**
   * Bootstrap the DSL for defining a Cookie to add
   *
   * @param name the name of the cookie, expressed as a function
   * @param value the value of the cookie, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static AddCookie Cookie(
      @NonNull Function<Session, String> name, @NonNull Function<Session, String> value) {
    return new AddCookie(
        io.gatling.http.Predef.Cookie(
            javaFunctionToExpression(name), javaFunctionToExpression(value)));
  }

  /**
   * Bootstrap the DSL for defining a Cookie to get
   *
   * @param name the name of the cookie, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public static GetCookie CookieKey(@NonNull String name) {
    return new GetCookie(io.gatling.http.Predef.CookieKey(toStringExpression(name)));
  }

  /**
   * Bootstrap the DSL for defining a Cookie to get
   *
   * @param name the name of the cookie, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public static GetCookie CookieKey(@NonNull Function<Session, String> name) {
    return new GetCookie(io.gatling.http.Predef.CookieKey(javaFunctionToExpression(name)));
  }

  ////////// ProxySupport
  /**
   * Bootstrap the DSL for defining a Proxy
   *
   * @param host the proxy host
   * @param port the proxy prot
   * @return the next DSL step
   */
  @NonNull
  public static Proxy Proxy(@NonNull String host, int port) {
    return new Proxy(io.gatling.http.Predef.Proxy(host, port));
  }
}
