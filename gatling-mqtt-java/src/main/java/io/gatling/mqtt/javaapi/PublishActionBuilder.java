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

package io.gatling.mqtt.javaapi;

import static io.gatling.core.javaapi.internal.Converters.*;
import static io.gatling.core.javaapi.internal.Expressions.*;

import io.gatling.core.javaapi.ActionBuilder;
import io.gatling.core.javaapi.Body;
import io.gatling.core.javaapi.CheckBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.mqtt.javaapit.internal.MqttChecks;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * DSL for actions that publish MQTT messages
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class PublishActionBuilder implements ActionBuilder {

  private final io.gatling.mqtt.action.builder.PublishBuilder wrapped;

  public static final class Base {
    private final io.gatling.mqtt.action.builder.MqttActionPublishBase wrapped;

    Base(io.gatling.mqtt.action.builder.MqttActionPublishBase wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Provide the message to send
     *
     * @param body a body
     * @return a new ActionBuilder instance
     */
    @Nonnull
    public PublishActionBuilder message(@Nonnull Body body) {
      return new PublishActionBuilder(wrapped.message(body.asScala()));
    }
  }

  public PublishActionBuilder(io.gatling.mqtt.action.builder.PublishBuilder wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Instruct to use an at-most-once QoS
   *
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public PublishActionBuilder qosAtMostOnce() {
    return new PublishActionBuilder(wrapped.qosAtMostOnce());
  }

  /**
   * Instruct to use an at-least-once QoS
   *
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public PublishActionBuilder qosAtLeastOnce() {
    return new PublishActionBuilder(wrapped.qosAtLeastOnce());
  }

  /**
   * Instruct to use an exactly-once QoS
   *
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public PublishActionBuilder qosExactlyOnce() {
    return new PublishActionBuilder(wrapped.qosExactlyOnce());
  }

  public static class Checkable implements ActionBuilder {

    private final io.gatling.mqtt.action.builder.PublishBuilder wrapped;

    private Checkable(io.gatling.mqtt.action.builder.PublishBuilder wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Instruct to use an at-most-once QoS
     *
     * @return a new ActionBuilder instance
     */
    @Nonnull
    public PublishActionBuilder qosAtMostOnce() {
      return new PublishActionBuilder(wrapped.qosAtMostOnce());
    }

    /**
     * Instruct to use an at-least-once QoS
     *
     * @return a new ActionBuilder instance
     */
    @Nonnull
    public PublishActionBuilder qosAtLeastOnce() {
      return new PublishActionBuilder(wrapped.qosAtLeastOnce());
    }

    /**
     * Instruct to use an exactly-once QoS
     *
     * @return a new ActionBuilder instance
     */
    @Nonnull
    public PublishActionBuilder qosExactlyOnce() {
      return new PublishActionBuilder(wrapped.qosExactlyOnce());
    }

    /**
     * Apply some checks
     *
     * @param checkBuilders the checks
     * @return the next DSL step
     */
    @Nonnull
    public Checkable check(@Nonnull CheckBuilder... checkBuilders) {
      return check(Arrays.asList(checkBuilders));
    }

    /**
     * Apply some checks
     *
     * @param checkBuilders the checks
     * @return the next DSL step
     */
    @Nonnull
    public Checkable check(@Nonnull List<CheckBuilder> checkBuilders) {
      return new Checkable(
          ((io.gatling.mqtt.action.builder.CheckablePublishBuilder) wrapped)
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
   * @return the next DSL step
   */
  @Nonnull
  public Checkable wait(int timeout) {
    return wait(Duration.ofSeconds(timeout));
  }

  /**
   * Wait for the checks to complete
   *
   * @param timeout the check timeout
   * @return the next DSL step
   */
  @Nonnull
  public Checkable wait(@Nonnull Duration timeout) {
    return new Checkable(wrapped.wait(toScalaDuration(timeout)));
  }

  /**
   * Wait for the checks to complete
   *
   * @param timeout the check timeout in seconds
   * @param expectedTopic the topic where the response message is expected to be published,
   *     expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public Checkable wait(int timeout, @Nonnull String expectedTopic) {
    return new Checkable(
        wrapped.wait(
            toScalaDuration(Duration.ofSeconds(timeout)), toStringExpression(expectedTopic)));
  }

  /**
   * Wait for the checks to complete
   *
   * @param timeout the check timeout
   * @param expectedTopic the topic where the response message is expected to be published,
   *     expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public Checkable wait(Duration timeout, @Nonnull String expectedTopic) {
    return new Checkable(wrapped.wait(toScalaDuration(timeout), toStringExpression(expectedTopic)));
  }

  /**
   * Wait for the checks to complete
   *
   * @param timeout the check timeout in seconds
   * @param expectedTopic the topic where the response message is expected to be published,
   *     expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public Checkable wait(@Nonnull int timeout, @Nonnull Function<Session, String> expectedTopic) {
    return new Checkable(
        wrapped.wait(
            toScalaDuration(Duration.ofSeconds(timeout)), javaFunctionToExpression(expectedTopic)));
  }

  /**
   * Wait for the checks to complete
   *
   * @param timeout the check timeout
   * @param expectedTopic the topic where the response message is expected to be published,
   *     expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public Checkable wait(
      @Nonnull Duration timeout, @Nonnull Function<Session, String> expectedTopic) {
    return new Checkable(
        wrapped.wait(toScalaDuration(timeout), javaFunctionToExpression(expectedTopic)));
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

  /**
   * Perform checks in the background, meaning state will have to be reconciled with {@link
   * MqttDsl#waitForMessages()}
   *
   * @param timeout the check timeout in seconds
   * @param expectedTopic the topic where the response message is expected to be published,
   *     expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public Checkable expect(int timeout, @Nonnull String expectedTopic) {
    return new Checkable(
        wrapped.expect(
            toScalaDuration(Duration.ofSeconds(timeout)), toStringExpression(expectedTopic)));
  }

  /**
   * Perform checks in the background, meaning state will have to be reconciled with {@link
   * MqttDsl#waitForMessages()}
   *
   * @param timeout the check timeout
   * @param expectedTopic the topic where the response message is expected to be published,
   *     expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public Checkable expect(
      @Nonnull Duration timeout, @Nonnull Function<Session, String> expectedTopic) {
    return new Checkable(
        wrapped.expect(toScalaDuration(timeout), javaFunctionToExpression(expectedTopic)));
  }

  /**
   * Perform checks in the background, meaning state will have to be reconciled with {@link
   * MqttDsl#waitForMessages()}
   *
   * @param timeout the check timeout in seconds
   * @param expectedTopic the topic where the response message is expected to be published,
   *     expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public Checkable expect(int timeout, @Nonnull Function<Session, String> expectedTopic) {
    return new Checkable(
        wrapped.expect(
            toScalaDuration(Duration.ofSeconds(timeout)), javaFunctionToExpression(expectedTopic)));
  }

  /**
   * Perform checks in the background, meaning state will have to be reconciled with {@link
   * MqttDsl#waitForMessages()}
   *
   * @param timeout the check timeout
   * @param expectedTopic the topic where the response message is expected to be published,
   *     expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public Checkable expect(@Nonnull Duration timeout, @Nonnull String expectedTopic) {
    return new Checkable(
        wrapped.expect(toScalaDuration(timeout), toStringExpression(expectedTopic)));
  }

  @Override
  public io.gatling.core.action.builder.ActionBuilder asScala() {
    return wrapped;
  }
}
