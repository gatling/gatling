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

import static io.gatling.javaapi.core.internal.Expressions.*;

import io.gatling.commons.validation.Validation;
import io.gatling.javaapi.core.Session;
import java.util.function.Function;
import javax.annotation.Nonnull;
import scala.Function1;

public final class Mqtt {

  private final io.gatling.mqtt.action.builder.MqttActionBuilderBase wrapped;

  Mqtt(Function1<io.gatling.core.session.Session, Validation<String>> name) {
    wrapped = new io.gatling.mqtt.action.builder.MqttActionBuilderBase(name);
  }

  /**
   * Create a builder for actions that create a MQTT connection
   *
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public ConnectActionBuilder connect() {
    return new ConnectActionBuilder(wrapped.connect());
  }

  /**
   * Create a builder for actions that subscribe to a MQTT topic
   *
   * @param topic the name of the topic, expressed as a Gatling Expression Language String
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public SubscribeActionBuilder subscribe(@Nonnull String topic) {
    return new SubscribeActionBuilder(wrapped.subscribe(toStringExpression(topic)));
  }

  /**
   * Create a builder for actions that subscribe to a MQTT topic
   *
   * @param topic the name of the topic, expressed as a function
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public SubscribeActionBuilder subscribe(@Nonnull Function<Session, String> topic) {
    return new SubscribeActionBuilder(wrapped.subscribe(javaFunctionToExpression(topic)));
  }

  /**
   * Create a builder for actions that publish to a MQTT topic
   *
   * @param topic the name of the topic, expressed as a Gatling Expression Language String
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public PublishActionBuilder.Base publish(@Nonnull String topic) {
    return new PublishActionBuilder.Base(wrapped.publish(toStringExpression(topic)));
  }

  /**
   * Create a builder for actions that publish to a MQTT topic
   *
   * @param topic the name of the topic, expressed as a function
   * @return a new ActionBuilder instance
   */
  @Nonnull
  public PublishActionBuilder.Base publish(@Nonnull Function<Session, String> topic) {
    return new PublishActionBuilder.Base(wrapped.publish(javaFunctionToExpression(topic)));
  }
}
