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
import io.gatling.core.Predef$;
import io.gatling.core.action.builder.ActionBuilder;
import io.gatling.core.action.builder.SessionHookBuilder;
import io.gatling.core.javaapi.CheckBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.http.action.sse.Sse$;
import io.gatling.http.action.ws.Ws$;
import io.gatling.http.javaapi.internal.HttpCheckType;
import io.gatling.http.protocol.HttpProtocolBuilder$;
import scala.Function1;

import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public final class Predef {
  private Predef() {
  }

  ////////// HttpDsl
  public static HttpProtocolBuilder http() {
    return new HttpProtocolBuilder(
      HttpProtocolBuilder$.MODULE$.apply(io.gatling.core.Predef$.MODULE$.configuration())
    );
  }

  public static Http http(String name) {
    return new Http(toStringExpression(name));
  }

  public static Http http(Function<Session, String> name) {
      return new Http(toTypedGatlingSessionFunction(name));
  }

  public static Ws ws(String name) {
    return new Ws(Ws$.MODULE$.apply(toStringExpression(name)));
  }

  public static Ws ws(String name, String wsName) {
    return new Ws(Ws$.MODULE$.apply(toStringExpression(name), toStringExpression(wsName)));
  }

  public static final Ws.Prefix ws = Ws.Prefix.INSTANCE;

  public static Sse sse(String name) {
    return new Sse(Sse$.MODULE$.apply(toStringExpression(name)));
  }

  public static Sse sse(String name, String sseName) {
    return new Sse(Sse$.MODULE$.apply(toStringExpression(name), toStringExpression(sseName)));
  }

  public static Polling poll() {
    return Polling.DEFAULT;
  }

  ////////// HttpCheckSupport
  public static CheckBuilder.Find<Integer> status() {
    return new CheckBuilder.Find.Default<>(io.gatling.http.Predef$.MODULE$.status(), HttpCheckType.Status, Integer.class::cast);
  }

  public static CheckBuilder.Find<String> currentLocation() {
    return new CheckBuilder.Find.Default<>(io.gatling.http.Predef$.MODULE$.currentLocation(), HttpCheckType.CurrentLocation, Function.identity());
  }

  public static CheckBuilder.CaptureGroupCheckBuilder currentLocationRegex(String pattern) {
    return new HttpCheckBuilder.CurrentLocationRegex(io.gatling.http.Predef$.MODULE$.currentLocationRegex(toStringExpression(pattern), Predef$.MODULE$.defaultPatterns()));
  }

  public static HttpCheckBuilder.CurrentLocationRegex currentLocationRegex(Function<Session, String> pattern) {
    return new HttpCheckBuilder.CurrentLocationRegex(io.gatling.http.Predef$.MODULE$.currentLocationRegex(toTypedGatlingSessionFunction(pattern), Predef$.MODULE$.defaultPatterns()));
  }

  public static CheckBuilder.MultipleFind<String> header(CharSequence name) {
    return new CheckBuilder.MultipleFind.Default<>(io.gatling.http.Predef$.MODULE$.header(toStaticValueExpression(name)), HttpCheckType.Header, Function.identity());
  }

  public static CheckBuilder.MultipleFind<String> header(String name) {
    return new CheckBuilder.MultipleFind.Default<>(io.gatling.http.Predef$.MODULE$.header(toExpression(name, CharSequence.class)), HttpCheckType.Header, Function.identity());
  }

  public static CheckBuilder.MultipleFind<String> header(Function<Session, CharSequence> name) {
    return new CheckBuilder.MultipleFind.Default<>(io.gatling.http.Predef$.MODULE$.header(toTypedGatlingSessionFunction(name)), HttpCheckType.Header, Function.identity());
  }

  public static CheckBuilder.CaptureGroupCheckBuilder headerRegex(CharSequence name, String pattern) {
    return new HttpCheckBuilder.HeaderRegexCheck(io.gatling.http.Predef$.MODULE$.headerRegex(toStaticValueExpression(name), toStringExpression(pattern), Predef$.MODULE$.defaultPatterns()));
  }

  public static CheckBuilder.CaptureGroupCheckBuilder headerRegex(String name, String pattern) {
    return new HttpCheckBuilder.HeaderRegexCheck(io.gatling.http.Predef$.MODULE$.headerRegex(toExpression(name, CharSequence.class), toStringExpression(pattern), Predef$.MODULE$.defaultPatterns()));
  }

  public static CheckBuilder.CaptureGroupCheckBuilder headerRegex(Function<Session, CharSequence> name, String pattern) {
    return new HttpCheckBuilder.HeaderRegexCheck(io.gatling.http.Predef$.MODULE$.headerRegex(toTypedGatlingSessionFunction(name), toStringExpression(pattern), Predef$.MODULE$.defaultPatterns()));
  }

  public static CheckBuilder.CaptureGroupCheckBuilder headerRegex(CharSequence name, Function<Session, String> pattern) {
    return new HttpCheckBuilder.HeaderRegexCheck(io.gatling.http.Predef$.MODULE$.headerRegex(toStaticValueExpression(name), toTypedGatlingSessionFunction(pattern), Predef$.MODULE$.defaultPatterns()));
  }

  public static CheckBuilder.CaptureGroupCheckBuilder headerRegex(String name, Function<Session, String> pattern) {
    return new HttpCheckBuilder.HeaderRegexCheck(io.gatling.http.Predef$.MODULE$.headerRegex(toExpression(name, CharSequence.class), toTypedGatlingSessionFunction(pattern), Predef$.MODULE$.defaultPatterns()));
  }

  public static CheckBuilder.CaptureGroupCheckBuilder headerRegex(Function<Session, CharSequence> name, Function<Session, String> pattern) {
    return new HttpCheckBuilder.HeaderRegexCheck(io.gatling.http.Predef$.MODULE$.headerRegex(toTypedGatlingSessionFunction(name), toTypedGatlingSessionFunction(pattern), Predef$.MODULE$.defaultPatterns()));
  }

  ////////// SitemapFeederSupport
  public static io.gatling.core.feeder.SourceFeederBuilder<String> sitemap(String fileName) {
    return io.gatling.http.Predef$.MODULE$.sitemap(fileName, io.gatling.core.Predef$.MODULE$.configuration());
  }

  ////////// BodyPartSupport
  public static BodyPart ElFileBodyPart(String filePath) {
    return ElFileBodyPart(toStringExpression(filePath));
  }

  public static BodyPart ElFileBodyPart(Function<Session, String> filePath) {
    return ElFileBodyPart(toTypedGatlingSessionFunction(filePath));
  }

  public static BodyPart ElFileBodyPart(Function1<io.gatling.core.session.Session, Validation<String>> filePath) {
    return new BodyPart(
      io.gatling.http.Predef$.MODULE$.ElFileBodyPart(
        filePath,
        io.gatling.core.Predef$.MODULE$.configuration(),
        io.gatling.core.Predef$.MODULE$.elFileBodies()
      )
    );
  }

  public static BodyPart ElFileBodyPart(String name, String filePath) {
    return ElFileBodyPart(toStringExpression(name), toStringExpression(filePath));
  }

  public static BodyPart ElFileBodyPart(String name, Function<Session, String> filePath) {
    return ElFileBodyPart(toStringExpression(name), toTypedGatlingSessionFunction(filePath));
  }

  public static BodyPart ElFileBodyPart(Function<Session, String> name, String filePath) {
    return ElFileBodyPart(toTypedGatlingSessionFunction(name), toStringExpression(filePath));
  }

  public static BodyPart ElFileBodyPart(Function<Session, String> name, Function<Session, String> filePath) {
    return ElFileBodyPart(toTypedGatlingSessionFunction(name), toTypedGatlingSessionFunction(filePath));
  }

  public static BodyPart ElFileBodyPart(Function1<io.gatling.core.session.Session, Validation<String>> name, Function1<io.gatling.core.session.Session, Validation<String>> filePath) {
    return new BodyPart(
      io.gatling.http.Predef$.MODULE$.ElFileBodyPart(
        name,
        filePath,
        io.gatling.core.Predef$.MODULE$.configuration(),
        io.gatling.core.Predef$.MODULE$.elFileBodies()
      )
    );
  }

  public static BodyPart StringBodyPart(String string) {
    return StringBodyPart(toStringExpression(string));
  }

  public static BodyPart StringBodyPart(Function<Session, String> string) {
    return ElFileBodyPart(toTypedGatlingSessionFunction(string));
  }

  public static BodyPart StringBodyPart(Function1<io.gatling.core.session.Session, Validation<String>> string) {
    return new BodyPart(
      io.gatling.http.Predef$.MODULE$.StringBodyPart(
        string,
        io.gatling.core.Predef$.MODULE$.configuration()
      )
    );
  }

  public static BodyPart StringBodyPart(String name, String string) {
    return StringBodyPart(toStringExpression(name), toStringExpression(string));
  }

  public static BodyPart StringBodyPart(String name, Function<Session, String> string) {
    return StringBodyPart(toStringExpression(name), toTypedGatlingSessionFunction(string));
  }

  public static BodyPart StringBodyPart(Function<Session, String> name, String string) {
    return StringBodyPart(toTypedGatlingSessionFunction(name), toStringExpression(string));
  }

  public static BodyPart StringBodyPart(Function<Session, String> name, Function<Session, String> string) {
    return StringBodyPart(toTypedGatlingSessionFunction(name), toTypedGatlingSessionFunction(string));
  }

  public static BodyPart StringBodyPart(Function1<io.gatling.core.session.Session, Validation<String>> name, Function1<io.gatling.core.session.Session, Validation<String>> string) {
    return new BodyPart(
      io.gatling.http.Predef$.MODULE$.StringBodyPart(
        name,
        string,
        io.gatling.core.Predef$.MODULE$.configuration()
      )
    );
  }

  public static BodyPart RawFileBodyPart(String filePath) {
    return RawFileBodyPart(toStringExpression(filePath));
  }

  public static BodyPart RawFileBodyPart(Function<Session, String> filePath) {
    return RawFileBodyPart(toTypedGatlingSessionFunction(filePath));
  }

  public static BodyPart RawFileBodyPart(Function1<io.gatling.core.session.Session, Validation<String>> filePath) {
    return new BodyPart(
      io.gatling.http.Predef$.MODULE$.RawFileBodyPart(
        filePath,
        io.gatling.core.Predef$.MODULE$.rawFileBodies()
      )
    );
  }

  public static BodyPart RawFileBodyPart(String name, String filePath) {
    return RawFileBodyPart(toStringExpression(name), toStringExpression(filePath));
  }

  public static BodyPart RawFileBodyPart(String name, Function<Session, String> filePath) {
    return RawFileBodyPart(toStringExpression(name), toTypedGatlingSessionFunction(filePath));
  }

  public static BodyPart RawFileBodyPart(Function<Session, String> name, String filePath) {
    return RawFileBodyPart(toTypedGatlingSessionFunction(name), toStringExpression(filePath));
  }

  public static BodyPart RawFileBodyPart(Function<Session, String> name, Function<Session, String> filePath) {
    return RawFileBodyPart(toTypedGatlingSessionFunction(name), toTypedGatlingSessionFunction(filePath));
  }

  public static BodyPart RawFileBodyPart(Function1<io.gatling.core.session.Session, Validation<String>> name, Function1<io.gatling.core.session.Session, Validation<String>> filePath) {
    return new BodyPart(
      io.gatling.http.Predef$.MODULE$.RawFileBodyPart(
        name,
        filePath,
        io.gatling.core.Predef$.MODULE$.rawFileBodies()
      )
    );
  }

  public static BodyPart PebbleFileBodyPart(String filePath) {
    return PebbleFileBodyPart(toStringExpression(filePath));
  }

  public static BodyPart PebbleFileBodyPart(Function<Session, String> filePath) {
    return PebbleFileBodyPart(toTypedGatlingSessionFunction(filePath));
  }

  public static BodyPart PebbleFileBodyPart(Function1<io.gatling.core.session.Session, Validation<String>> filePath) {
    return new BodyPart(
      io.gatling.http.Predef$.MODULE$.PebbleFileBodyPart(
        filePath,
        io.gatling.core.Predef$.MODULE$.configuration(),
        io.gatling.core.Predef$.MODULE$.pebbleFileBodies()
      )
    );
  }

  public static BodyPart PebbleFileBodyPart(String name, String filePath) {
    return PebbleFileBodyPart(toStringExpression(name), toStringExpression(filePath));
  }

  public static BodyPart PebbleFileBodyPart(String name, Function<Session, String> filePath) {
    return PebbleFileBodyPart(toStringExpression(name), toTypedGatlingSessionFunction(filePath));
  }

  public static BodyPart PebbleFileBodyPart(Function<Session, String> name, String filePath) {
    return PebbleFileBodyPart(toTypedGatlingSessionFunction(name), toStringExpression(filePath));
  }

  public static BodyPart PebbleFileBodyPart(Function<Session, String> name, Function<Session, String> filePath) {
    return PebbleFileBodyPart(toTypedGatlingSessionFunction(name), toTypedGatlingSessionFunction(filePath));
  }

  public static BodyPart PebbleFileBodyPart(Function1<io.gatling.core.session.Session, Validation<String>> name, Function1<io.gatling.core.session.Session, Validation<String>> filePath) {
    return new BodyPart(
      io.gatling.http.Predef$.MODULE$.PebbleFileBodyPart(
        name,
        filePath,
        io.gatling.core.Predef$.MODULE$.configuration(),
        io.gatling.core.Predef$.MODULE$.pebbleFileBodies()
      )
    );
  }

  public static BodyPart PebbleStringBodyPart(String string) {
    return new BodyPart(
      io.gatling.http.Predef$.MODULE$.PebbleStringBodyPart(
        string,
        io.gatling.core.Predef$.MODULE$.configuration()
      )
    );
  }

  public static BodyPart PebbleStringBodyPart(String name, String string) {
    return PebbleStringBodyPart(toStringExpression(name), string);
  }

  public static BodyPart PebbleStringBodyPart(Function<Session, String> name, String string) {
    return PebbleStringBodyPart(toTypedGatlingSessionFunction(name), string);
  }

  public static BodyPart PebbleStringBodyPart(Function1<io.gatling.core.session.Session, Validation<String>> name, String string) {
    return new BodyPart(
      io.gatling.http.Predef$.MODULE$.PebbleStringBodyPart(
        name,
        string,
        io.gatling.core.Predef$.MODULE$.configuration()
      )
    );
  }

  public static BodyPart ByteArrayBodyPart(String name, byte[] bytes) {
    return ByteArrayBodyPart(toStringExpression(name), toStaticValueExpression(bytes));
  }

  public static BodyPart ByteArrayBodyPart(Function<Session, String> name, byte[] bytes) {
    return ByteArrayBodyPart(toTypedGatlingSessionFunction(name), toStaticValueExpression(bytes));
  }

  public static BodyPart ByteArrayBodyPart(String name, String bytes) {
    return ByteArrayBodyPart(toStringExpression(name), toBytesExpression(bytes));
  }

  public static BodyPart ByteArrayBodyPart(Function<Session, String> name, String bytes) {
    return ByteArrayBodyPart(toTypedGatlingSessionFunction(name), toBytesExpression(bytes));
  }

  public static BodyPart ByteArrayBodyPart(String name, Function<Session, byte[]> bytes) {
    return ByteArrayBodyPart(toStringExpression(name), toTypedGatlingSessionFunction(bytes));
  }

  public static BodyPart ByteArrayBodyPart(Function<Session, String> name, Function<Session, byte[]> bytes) {
    return ByteArrayBodyPart(toTypedGatlingSessionFunction(name), toTypedGatlingSessionFunction(bytes));
  }

  public static BodyPart ByteArrayBodyPart(Function1<io.gatling.core.session.Session, Validation<String>> name, Function1<io.gatling.core.session.Session, Validation<byte[]>> bytes) {
    return new BodyPart(
      io.gatling.http.Predef$.MODULE$.ByteArrayBodyPart(
        name,
        bytes
      )
    );
  }

  ////////// CookieSupport
  public static ActionBuilder addCookie(AddCookie cookie) {
    return io.gatling.http.action.cookie.AddCookieBuilder$.MODULE$.apply(cookie.asScala());
  }

  public static ActionBuilder getCookieValue(GetCookie cookie) {
    return io.gatling.http.action.cookie.GetCookieBuilder$.MODULE$.apply(cookie.asScala());
  }

  public static ActionBuilder flushSessionCookies() {
    return new SessionHookBuilder(io.gatling.http.Predef$.MODULE$.flushSessionCookies(), true);
  }
  public static ActionBuilder flushCookieJar() {
    return new SessionHookBuilder(io.gatling.http.Predef$.MODULE$.flushCookieJar(), true);
  }
  public static ActionBuilder flushHttpCache() {
    return new SessionHookBuilder(io.gatling.http.Predef$.MODULE$.flushHttpCache(), true);
  }

  public static AddCookie Cookie(String name, String value) {
    return new AddCookie(io.gatling.http.Predef$.MODULE$.Cookie(toStringExpression(name), toStringExpression(value)));
  }

  public static AddCookie Cookie(Function<Session, String> name, String value) {
    return new AddCookie(io.gatling.http.Predef$.MODULE$.Cookie(toTypedGatlingSessionFunction(name), toStringExpression(value)));
  }

  public static AddCookie Cookie(String name, Function<Session, String> value) {
    return new AddCookie(io.gatling.http.Predef$.MODULE$.Cookie(toStringExpression(name), toTypedGatlingSessionFunction(value)));
  }

  public static AddCookie Cookie(Function<Session, String> name, Function<Session, String> value) {
    return new AddCookie(io.gatling.http.Predef$.MODULE$.Cookie(toTypedGatlingSessionFunction(name), toTypedGatlingSessionFunction(value)));
  }

  public static GetCookie CookieKey(String name) {
    return new GetCookie(io.gatling.http.Predef$.MODULE$.CookieKey(toStringExpression(name)));
  }

  public static GetCookie CookieKey(Function<Session, String> name) {
    return new GetCookie(io.gatling.http.Predef$.MODULE$.CookieKey(toTypedGatlingSessionFunction(name)));
  }

  ////////// ProxySupport
  public static Proxy Proxy(String host, int port) {
    return new Proxy(io.gatling.http.Predef$.MODULE$.Proxy(host, port));
  }
}
