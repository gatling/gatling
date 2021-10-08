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

package io.gatling.javaapi.http;

import static io.gatling.javaapi.core.internal.Converters.*;

import io.gatling.http.request.builder.polling.PollingEveryStep;
import io.gatling.javaapi.core.ActionBuilder;
import java.time.Duration;
import javax.annotation.Nonnull;

/**
 * DSL for building HTTP polling configurations
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class Polling {
  static final Polling DEFAULT =
      new Polling(io.gatling.http.request.builder.polling.Polling.Default());

  private final io.gatling.http.request.builder.polling.Polling wrapped;

  Polling(io.gatling.http.request.builder.polling.Polling wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Define a custom poller name so multiple pollers for the same virtual users don't conflict
   *
   * @param pollerName the name
   * @return the next DSL step
   */
  @Nonnull
  public Polling pollerName(@Nonnull String pollerName) {
    return new Polling(wrapped.pollerName(pollerName));
  }

  /**
   * Define the polling period
   *
   * @param period the period in seconds
   * @return the next DSL step
   */
  @Nonnull
  public Every every(int period) {
    return new Every(wrapped.every(toScalaDuration(Duration.ofSeconds(period))));
  }

  /**
   * Define the polling period
   *
   * @param period the period
   * @return the next DSL step
   */
  @Nonnull
  public Every every(@Nonnull Duration period) {
    return new Every(wrapped.every(toScalaDuration(period)));
  }

  public static class Every {
    private final io.gatling.http.request.builder.polling.PollingEveryStep wrapped;

    public Every(PollingEveryStep wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the polling request
     *
     * @return an ActionBuilder
     */
    @Nonnull
    public ActionBuilder exec(@Nonnull HttpRequestActionBuilder requestBuilder) {
      return () -> wrapped.exec(requestBuilder.wrapped);
    }
  }

  /**
   * Stop polling
   *
   * @return an ActionBuilder
   */
  @Nonnull
  public ActionBuilder stop() {
    return wrapped::stop;
  }
}
