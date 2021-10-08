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

package io.gatling.javaapi.mqtt;

import static io.gatling.javaapi.core.internal.Converters.*;

import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.mqtt.internal.MqttChecks;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * DSL for actions that subscribe to MQTT topics
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class SubscribeActionBuilder implements ActionBuilder {

  private final io.gatling.mqtt.action.builder.SubscribeBuilder wrapped;

  SubscribeActionBuilder(io.gatling.mqtt.action.builder.SubscribeBuilder wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Instruct to use an at-most-once QoS
   *
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public SubscribeActionBuilder qosAtMostOnce() {
    return new SubscribeActionBuilder(wrapped.qosAtMostOnce());
  }

  /**
   * Instruct to use an at-least-once QoS
   *
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public SubscribeActionBuilder qosAtLeastOnce() {
    return new SubscribeActionBuilder(wrapped.qosAtLeastOnce());
  }

  /**
   * Instruct to use an exactly-once QoS
   *
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public SubscribeActionBuilder qosExactlyOnce() {
    return new SubscribeActionBuilder(wrapped.qosExactlyOnce());
  }

  public static final class Checkable implements ActionBuilder {

    private final io.gatling.mqtt.action.builder.SubscribeBuilder wrapped;

    Checkable(io.gatling.mqtt.action.builder.SubscribeBuilder wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Instruct to use an at-most-once QoS
     *
     * @return a new ActionBuilder instance
     */
    @Nonnull
    public SubscribeActionBuilder qosAtMostOnce() {
      return new SubscribeActionBuilder(wrapped.qosAtMostOnce());
    }

    /**
     * Instruct to use an at-least-once QoS
     *
     * @return a new ActionBuilder instance
     */
    @Nonnull
    public SubscribeActionBuilder qosAtLeastOnce() {
      return new SubscribeActionBuilder(wrapped.qosAtLeastOnce());
    }

    /**
     * Instruct to use an exactly-once QoS
     *
     * @return a new ActionBuilder instance
     */
    @Nonnull
    public SubscribeActionBuilder qosExactlyOnce() {
      return new SubscribeActionBuilder(wrapped.qosExactlyOnce());
    }

    /**
     * Apply some checks
     *
     * @param checkBuilders the checks
     * @return a new ActionBuilder instance
     */
    @Nonnull
    public Checkable check(@Nonnull CheckBuilder... checkBuilders) {
      return check(Arrays.asList(checkBuilders));
    }

    /**
     * Apply some checks
     *
     * @param checkBuilders the checks
     * @return a new ActionBuilder instance
     */
    @Nonnull
    public Checkable check(@Nonnull List<CheckBuilder> checkBuilders) {
      return new Checkable(
          ((io.gatling.mqtt.action.builder.CheckableSubscribeBuilder) wrapped)
              .check(MqttChecks.toScalaChecks(checkBuilders)));
    }

    @Override
    public io.gatling.core.action.builder.ActionBuilder asScala() {
      return wrapped;
    }
  }

  /**
   * Wait for the checks to complete
   *
   * @param timeout the check timeout in seconds
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public Checkable wait(int timeout) {
    return wait(Duration.ofSeconds(timeout));
  }

  /**
   * Wait for the checks to complete
   *
   * @param timeout the check timeout
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public Checkable wait(@Nonnull Duration timeout) {
    return new Checkable(wrapped.wait(toScalaDuration(timeout)));
  }

  /**
   * Perform checks in the background, meaning state will have to be reconciled with {@link
   * MqttDsl#waitForMessages()}
   *
   * @param timeout the check timeout in seconds
   * @return the next DSL step
   */
  @Nonnull
  public Checkable expect(int timeout) {
    return expect(Duration.ofSeconds(timeout));
  }

  /**
   * Perform checks in the background, meaning state will have to be reconciled with {@link
   * MqttDsl#waitForMessages()}
   *
   * @param timeout the check timeout
   * @return the next DSL step
   */
  @Nonnull
  public Checkable expect(@Nonnull Duration timeout) {
    return new Checkable(wrapped.expect(toScalaDuration(timeout)));
  }

  @Override
  public io.gatling.core.action.builder.ActionBuilder asScala() {
    return wrapped;
  }
}
