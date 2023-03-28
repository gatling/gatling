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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.commons.validation.Validation;
import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.Session;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import scala.Function1;
import scala.concurrent.duration.FiniteDuration;

public interface SseAwaitActionBuilder<
        T extends SseAwaitActionBuilder<T, W>,
        W extends io.gatling.http.action.sse.SseAwaitActionBuilder<W>>
    extends ActionBuilder {

  T make(Function<W, W> f);

  /**
   * Boostrap a check that waits for a given duration
   *
   * @param timeout the static wait duration in seconds
   * @return the next DSL step
   */
  @NonNull
  default On<T> await(long timeout) {
    return await(Duration.ofSeconds(timeout));
  }

  /**
   * Boostrap a check that waits for a given duration
   *
   * @param timeout the static wait duration
   * @return the next DSL step
   */
  @NonNull
  default On<T> await(@NonNull Duration timeout) {
    return new On<>(this, toStaticValueExpression(toScalaDuration(timeout)));
  }

  /**
   * Boostrap a check that waits for a given duration
   *
   * @param timeout the wait duration, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  default On<T> await(@NonNull String timeout) {
    return new On<>(this, toDurationExpression(timeout));
  }

  /**
   * Boostrap a check that waits for a given duration
   *
   * @param timeout the wait duration, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  default On<T> await(@NonNull Function<Session, Duration> timeout) {
    return new On<>(this, javaDurationFunctionToExpression(timeout));
  }

  final class On<T extends SseAwaitActionBuilder<T, ?>> {
    private final SseAwaitActionBuilder<T, ?> context;
    private final Function1<io.gatling.core.session.Session, Validation<FiniteDuration>> timeout;

    public On(
        SseAwaitActionBuilder<T, ?> context,
        Function1<io.gatling.core.session.Session, Validation<FiniteDuration>> timeout) {
      this.context = context;
      this.timeout = timeout;
    }

    /**
     * Define the checks to wait on
     *
     * @param checks the checks
     * @return a usable ActionBuilder
     */
    @NonNull
    public T on(@NonNull SseMessageCheck... checks) {
      return on(Arrays.asList(checks));
    }

    /**
     * Define the checks to wait on
     *
     * @param checks the checks
     * @return a usable ActionBuilder
     */
    @NonNull
    public T on(@NonNull List<SseMessageCheck> checks) {
      return context.make(
          wrapped ->
              wrapped.await(
                  timeout,
                  toScalaSeq(
                      checks.stream().map(SseMessageCheck::asScala).collect(Collectors.toList()))));
    }
  }
}
