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
import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.Session;
import java.util.function.Function;

/**
 * DSL for building WebSocket configurations
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class Ws {
  private final io.gatling.http.action.ws.Ws wrapped;

  Ws(final io.gatling.http.action.ws.Ws wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Define a custom WebSocket name so multiple WebSockets for the same virtual users don't conflict
   *
   * @param wsName the name, expressed as a Gatling Expression Language String
   * @return a new Ws instance
   */
  @NonNull
  public Ws wsName(@NonNull String wsName) {
    return new Ws(wrapped.wsName(toStringExpression(wsName)));
  }

  /**
   * Define a custom WebSocket name so multiple WebSockets for the same virtual users don't conflict
   *
   * @param wsName the name, expressed as a function
   * @return a new Ws instance
   */
  @NonNull
  public Ws wsName(@NonNull Function<Session, String> wsName) {
    return new Ws(wrapped.wsName(javaFunctionToExpression(wsName)));
  }

  /**
   * Boostrap an action to connect the WebSocket
   *
   * @param url the url to connect to, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public WsConnectActionBuilder connect(@NonNull String url) {
    return new WsConnectActionBuilder(wrapped.connect(toStringExpression(url)));
  }

  /**
   * Boostrap an action to connect the WebSocket
   *
   * @param url the url to connect to, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public WsConnectActionBuilder connect(@NonNull Function<Session, String> url) {
    return new WsConnectActionBuilder(wrapped.connect(javaFunctionToExpression(url)));
  }

  /**
   * Boostrap an action to send a TEXT frame
   *
   * @param text the text to send, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public WsSendTextActionBuilder sendText(@NonNull String text) {
    return new WsSendTextActionBuilder(wrapped.sendText(toStringExpression(text)));
  }

  /**
   * Boostrap an action to send a TEXT frame
   *
   * @param text the text to send, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public WsSendTextActionBuilder sendText(@NonNull Function<Session, String> text) {
    return new WsSendTextActionBuilder(wrapped.sendText(javaFunctionToExpression(text)));
  }

  /**
   * Boostrap an action to send a BINARY frame
   *
   * @param bytes the static bytes to send
   * @return the next DSL step
   */
  @NonNull
  public WsSendBinaryActionBuilder sendBytes(@NonNull byte[] bytes) {
    return new WsSendBinaryActionBuilder(wrapped.sendBytes(toStaticValueExpression(bytes)));
  }

  /**
   * Boostrap an action to send a BINARY frame
   *
   * @param bytes the bytes to send, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public WsSendBinaryActionBuilder sendBytes(@NonNull String bytes) {
    return new WsSendBinaryActionBuilder(wrapped.sendBytes(toBytesExpression(bytes)));
  }

  /**
   * Boostrap an action to send a BINARY frame
   *
   * @param bytes the bytes to send, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public WsSendBinaryActionBuilder sendBytes(@NonNull Function<Session, byte[]> bytes) {
    return new WsSendBinaryActionBuilder(wrapped.sendBytes(javaFunctionToExpression(bytes)));
  }

  /**
   * Boostrap an action to send a CLOSE frame with the default 1000 status code
   *
   * @return the next DSL step
   */
  @NonNull
  public ActionBuilder close() {
    return wrapped::close;
  }

  /**
   * Boostrap an action to send a CLOSE frame with specified status and reason
   *
   * @param statusCode the close frame status code
   * @param reason the close frame reason
   * @return the next DSL step
   */
  @NonNull
  public ActionBuilder close(int statusCode, String reason) {
    return () -> wrapped.close(statusCode, reason);
  }

  public static final class Prefix {

    public static final Prefix INSTANCE = new Prefix();

    private Prefix() {}

    /**
     * Bootstrap a check on inbound TEXT frames
     *
     * @param name the name of the check, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public WsFrameCheck.Text checkTextMessage(@NonNull String name) {
      return new WsFrameCheck.Text(
          io.gatling.http.Predef.ws().checkTextMessage(toStringExpression(name)));
    }

    /**
     * Bootstrap a check on inbound TEXT frames
     *
     * @param name the name of the check, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public WsFrameCheck.Text checkTextMessage(@NonNull Function<Session, String> name) {
      return new WsFrameCheck.Text(
          io.gatling.http.Predef.ws().checkTextMessage(javaFunctionToExpression(name)));
    }

    /**
     * Bootstrap a check on inbound BINARY frames
     *
     * @param name the name of the check, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public WsFrameCheck.Binary checkBinaryMessage(@NonNull String name) {
      return new WsFrameCheck.Binary(
          io.gatling.http.Predef.ws().checkBinaryMessage(toStringExpression(name)));
    }

    /**
     * Bootstrap a check on inbound BINARY frames
     *
     * @param name the name of the check, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public WsFrameCheck.Binary checkBinaryMessage(@NonNull Function<Session, String> name) {
      return new WsFrameCheck.Binary(
          io.gatling.http.Predef.ws().checkBinaryMessage(javaFunctionToExpression(name)));
    }
  }
}
