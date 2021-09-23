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

import io.gatling.core.action.builder.ActionBuilder;
import io.gatling.core.javaapi.Session;

import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public class Sse {
  private final io.gatling.http.action.sse.Sse wrapped;

  public Sse(final io.gatling.http.action.sse.Sse wrapped) {
    this.wrapped = wrapped;
  }

  public Sse sseName(String sseName) {
    return new Sse(wrapped.sseName(toStringExpression(sseName)));
  }

  public Sse sseName(Function<Session, String> sseName) {
    return new Sse(wrapped.sseName(toTypedGatlingSessionFunction(sseName)));
  }

  public SseConnectActionBuilder connect(String url) {
    return new SseConnectActionBuilder(wrapped.connect(toStringExpression(url)));
  }

  public SseConnectActionBuilder connect(Function<Session, String> url) {
    return new SseConnectActionBuilder(wrapped.connect(toTypedGatlingSessionFunction(url)));
  }

  public SseSetCheckActionBuilder setCheck() {
    return new SseSetCheckActionBuilder(wrapped.setCheck());
  }

  public ActionBuilder close() {
    return wrapped.close();
  }
}
