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

import static io.gatling.core.javaapi.internal.Expressions.*;

import io.gatling.core.javaapi.Body;
import io.gatling.core.javaapi.Session;
import java.util.function.Function;
import javax.annotation.Nonnull;

/** The entrypoint of the Gatling MQTT DSL */
public final class MqttDsl {
  private MqttDsl() {}

  /**
   * Bootstrap a new MQTT protocol builder
   *
   * @return a new builder instance
   */
  @Nonnull
  public static MqttProtocolBuilder mqtt() {
    return new MqttProtocolBuilder(
        io.gatling.mqtt.Predef.mqtt(io.gatling.core.Predef.configuration()));
  }

  /**
   * Bootstrap a builder for last will messages
   *
   * @param topic the topic to send last will messages to, expressed as a Gatling Expression
   *     Language String
   * @param message the last will message
   * @return the next DSL step
   */
  @Nonnull
  public static LastWillBuilder LastWill(@Nonnull String topic, @Nonnull Body message) {
    return new LastWillBuilder(
        io.gatling.mqtt.Predef.LastWill(toStringExpression(topic), message.asScala()));
  }

  /**
   * Bootstrap a builder for last will messages
   *
   * @param topic the topic to send last will messages to, expressed as a function
   * @param message the last will message
   * @return the next DSL step
   */
  @Nonnull
  public static LastWillBuilder LastWill(
      @Nonnull Function<Session, String> topic, @Nonnull Body message) {
    return new LastWillBuilder(
        io.gatling.mqtt.Predef.LastWill(javaFunctionToExpression(topic), message.asScala()));
  }

  /**
   * Boostrap a builder for a MQTT action
   *
   * @param name the action name, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public static Mqtt mqtt(@Nonnull String name) {
    return new Mqtt(toStringExpression(name));
  }

  /**
   * Boostrap a builder for a MQTT action
   *
   * @param name the action name, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public static Mqtt mqtt(@Nonnull Function<Session, String> name) {
    return new Mqtt(javaFunctionToExpression(name));
  }

  /**
   * Boostrap a builder for an action that waits for all awaited inbound messages to arrive
   *
   * @return the next DSL step
   */
  @Nonnull
  public static WaitForMessagesActionBuilder waitForMessages() {
    return WaitForMessagesActionBuilder.DEFAULT;
  }
}
