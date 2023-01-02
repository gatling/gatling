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

import static io.gatling.javaapi.core.internal.Expressions.*;

import io.gatling.core.action.builder.ActionBuilder;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * DSL for building WebSocket connect actions
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class WsConnectActionBuilder
    extends RequestActionBuilder<
        WsConnectActionBuilder, io.gatling.http.request.builder.ws.WsConnectRequestBuilder>
    implements WsAwaitActionBuilder<
        WsConnectActionBuilder, io.gatling.http.request.builder.ws.WsConnectRequestBuilder> {

  WsConnectActionBuilder(io.gatling.http.request.builder.ws.WsConnectRequestBuilder wrapped) {
    super(wrapped);
  }

  /**
   * Define a WebSocket subprotocol
   *
   * @param sub the subprotocol, expressed as a Gatling Expression Language String
   * @return a new WsConnectActionBuilder instance
   */
  @Nonnull
  public WsConnectActionBuilder subprotocol(@Nonnull String sub) {
    return new WsConnectActionBuilder(wrapped.subprotocol(toStringExpression(sub)));
  }

  /**
   * Define a WebSocket subprotocol
   *
   * @param sub the subprotocol, expressed as a function
   * @return a new WsConnectActionBuilder instance
   */
  @Nonnull
  public WsConnectActionBuilder subprotocol(@Nonnull Function<Session, String> sub) {
    return new WsConnectActionBuilder(wrapped.subprotocol(javaFunctionToExpression(sub)));
  }

  /**
   * Define a chain to execute when the WebSocket gets connected or re-connected
   *
   * @param chain the chain
   * @return a new WsConnectActionBuilder instance
   */
  @Nonnull
  public WsConnectActionBuilder onConnected(@Nonnull ChainBuilder chain) {
    return new WsConnectActionBuilder(wrapped.onConnected(chain.wrapped));
  }

  @Override
  public WsConnectActionBuilder make(
      Function<
              io.gatling.http.request.builder.ws.WsConnectRequestBuilder,
              io.gatling.http.request.builder.ws.WsConnectRequestBuilder>
          f) {
    return new WsConnectActionBuilder(f.apply(wrapped));
  }

  @Override
  public ActionBuilder asScala() {
    return wrapped;
  }
}
