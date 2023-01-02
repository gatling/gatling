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

package io.gatling.javaapi.mqtt;

import static io.gatling.javaapi.core.internal.Converters.*;

import io.gatling.javaapi.core.ActionBuilder;
import java.time.Duration;
import javax.annotation.Nonnull;

public final class WaitForMessagesActionBuilder implements ActionBuilder {

  public static final WaitForMessagesActionBuilder DEFAULT =
      new WaitForMessagesActionBuilder(
          io.gatling.mqtt.action.builder.WaitForMessagesBuilder.Default());

  private final io.gatling.mqtt.action.builder.WaitForMessagesBuilder wrapped;

  WaitForMessagesActionBuilder(io.gatling.mqtt.action.builder.WaitForMessagesBuilder wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Define the timeout for waiting for pending expects
   *
   * @param timeout the timeout in seconds
   * @return a new WaitForMessagesActionBuilder instance
   */
  @Nonnull
  public WaitForMessagesActionBuilder timeout(long timeout) {
    return timeout(Duration.ofSeconds(timeout));
  }

  /**
   * Define the timeout for waiting for pending expects
   *
   * @param timeout the timeout
   * @return a new WaitForMessagesActionBuilder instance
   */
  @Nonnull
  public WaitForMessagesActionBuilder timeout(@Nonnull Duration timeout) {
    return new WaitForMessagesActionBuilder(wrapped.timeout(toScalaDuration(timeout)));
  }

  @Override
  public io.gatling.core.action.builder.ActionBuilder asScala() {
    return wrapped;
  }
}
