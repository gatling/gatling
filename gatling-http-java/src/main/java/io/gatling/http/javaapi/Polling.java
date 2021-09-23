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
import io.gatling.http.request.builder.polling.PollingEveryStep;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

import java.time.Duration;

public final class Polling {
  static final Polling DEFAULT = new Polling(io.gatling.http.request.builder.polling.Polling.Default());

  private final io.gatling.http.request.builder.polling.Polling wrapped;

  public Polling(io.gatling.http.request.builder.polling.Polling wrapped) {
    this.wrapped = wrapped;
  }

  public Polling pollerName(String pollerName) {
    return new Polling(wrapped.pollerName(pollerName));
  }

  public Every every(int period) {
    return new Every(wrapped.every(toScalaDuration(Duration.ofSeconds(period))));
  }

  public Every every(Duration period) {
    return new Every(wrapped.every(toScalaDuration(period)));
  }

  public static class Every {
    private final io.gatling.http.request.builder.polling.PollingEveryStep wrapped;

    public Every(PollingEveryStep wrapped) {
      this.wrapped = wrapped;
    }

    public ActionBuilder exec(HttpRequestActionBuilder requestBuilder) {
      return wrapped.exec(requestBuilder.wrapped);
    }
  }

  public ActionBuilder stop() {
    return wrapped.stop();
  }
}
