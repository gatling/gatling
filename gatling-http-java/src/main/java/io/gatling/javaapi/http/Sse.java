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
 * DSL for building <a
 * href="https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events">SSE</a>
 * configurations
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class Sse {
  private final io.gatling.http.action.sse.Sse wrapped;

  Sse(final io.gatling.http.action.sse.Sse wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Define a custom stream name so multiple SSEs for the same virtual users don't conflict
   *
   * @param sseName the name, expressed as a Gatling Expression Language String
   * @return a new Sse instance
   */
  @NonNull
  public Sse sseName(@NonNull String sseName) {
    return new Sse(wrapped.sseName(toStringExpression(sseName)));
  }

  /**
   * Define a custom stream name so multiple SSEs for the same virtual users don't conflict
   *
   * @param sseName the name, expressed as a function
   * @return a new Sse instance
   */
  @NonNull
  public Sse sseName(@NonNull Function<Session, String> sseName) {
    return new Sse(wrapped.sseName(javaFunctionToExpression(sseName)));
  }

  /**
   * Boostrap an action to connect the SSE
   *
   * @param url the url to connect to, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public SseConnectActionBuilder connect(@NonNull String url) {
    return new SseConnectActionBuilder(wrapped.connect(toStringExpression(url)));
  }

  /**
   * Boostrap an action to connect the SSE
   *
   * @param url the url to connect to, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public SseConnectActionBuilder connect(@NonNull Function<Session, String> url) {
    return new SseConnectActionBuilder(wrapped.connect(javaFunctionToExpression(url)));
  }

  /**
   * Boostrap an action to set a check
   *
   * @return the next DSL step
   */
  @NonNull
  public SseSetCheckActionBuilder setCheck() {
    return new SseSetCheckActionBuilder(wrapped.setCheck());
  }

  /**
   * Create an action to close the stream
   *
   * @return an ActionBuilder
   */
  @NonNull
  public ActionBuilder close() {
    return wrapped::close;
  }

  public static final class Prefix {

    public static final Prefix INSTANCE = new Prefix();

    private Prefix() {}

    /**
     * Boostrap a SSE check
     *
     * @param name
     * @return the next DSL step
     */
    @NonNull
    public SseMessageCheck checkMessage(@NonNull String name) {
      return new SseMessageCheck(io.gatling.http.Predef.sse().checkMessage(name));
    }
  }
}
