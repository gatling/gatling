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

import io.gatling.core.action.Action;
import io.gatling.core.javaapi.ChainBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.core.structure.ScenarioContext;
import io.gatling.http.check.ws.WsFrameCheck;

import java.util.function.Function;
import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public final class WsConnectActionBuilder
  extends RequestActionBuilder<WsConnectActionBuilder, io.gatling.http.request.builder.ws.WsConnectRequestBuilder>
  implements WsAwaitActionBuilder<WsConnectActionBuilder, io.gatling.http.request.builder.ws.WsConnectRequestBuilder, WsFrameCheck> {

  public WsConnectActionBuilder(io.gatling.http.request.builder.ws.WsConnectRequestBuilder wrapped) {
    super(wrapped);
  }

  public WsConnectActionBuilder subprotocol(String sub) {
    return new WsConnectActionBuilder(wrapped.subprotocol(toStringExpression(sub)));
  }

  public WsConnectActionBuilder subprotocol(Function<Session, String> sub) {
    return new WsConnectActionBuilder(wrapped.subprotocol(toTypedGatlingSessionFunction(sub)));
  }

  public WsConnectActionBuilder onConnected(ChainBuilder chain) {
    return new WsConnectActionBuilder(wrapped.onConnected(chain.wrapped));
  }

  @Override
  public WsConnectActionBuilder make(Function<io.gatling.http.request.builder.ws.WsConnectRequestBuilder, io.gatling.http.request.builder.ws.WsConnectRequestBuilder> f) {
    return new WsConnectActionBuilder(f.apply(wrapped));
  }

  @Override
  public Action build(ScenarioContext ctx, Action next) {
    return wrapped.build(ctx, next);
  }
}
