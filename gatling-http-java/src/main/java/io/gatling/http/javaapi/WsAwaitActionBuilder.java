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
import io.gatling.core.action.builder.ActionBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.http.check.ws.WsFrameCheck;
import scala.Function1;
import scala.concurrent.duration.FiniteDuration;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public interface WsAwaitActionBuilder<T extends WsAwaitActionBuilder<T, W, C>, W extends io.gatling.http.action.ws.WsAwaitActionBuilder<W, C>, C extends WsFrameCheck> extends ActionBuilder {

  T make(Function<W, W> f);

  default On<T, C> await(int duration) {
    return await(Duration.ofSeconds(duration));
  }

  default On<T, C> await(String duration) {
    return new On<>(this, toDurationExpression(duration));
  }

  default On<T, C> await(Duration duration) {
    return new On<>(this, toStaticValueExpression(toScalaDuration(duration)));
  }

  default On<T, C> await(Function<Session, Duration> timeout) {
    return new On<>(this, toGatlingSessionFunctionDuration(timeout));
  }

  final class On<T extends WsAwaitActionBuilder<T, ?, C>, C extends WsFrameCheck> {
    private final WsAwaitActionBuilder<T, ?, C> context;
    private final Function1<io.gatling.core.session.Session, Validation<FiniteDuration>> timeout;

    public On(WsAwaitActionBuilder<T, ?, C> context, Function1<io.gatling.core.session.Session, Validation<FiniteDuration>> timeout) {
      this.context = context;
      this.timeout = timeout;
    }

    public T on(C... checks) {
      return context.make(wrapped -> wrapped.await(timeout, toScalaSeq(checks)));
    }

    public T on(List<C> checks) {
      return context.make(wrapped -> wrapped.await(timeout, toScalaSeq(checks)));
    }
  }
}
