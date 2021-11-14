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
import static io.gatling.javaapi.core.internal.Expressions.*;

import io.gatling.core.protocol.Protocol;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.ProtocolBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.mqtt.internal.MessageCorrelators;
import java.time.Duration;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.net.ssl.KeyManagerFactory;

/**
 * DSL for building MQTT protocol configuration
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class MqttProtocolBuilder implements ProtocolBuilder {
  private final io.gatling.mqtt.protocol.MqttProtocolBuilder wrapped;

  MqttProtocolBuilder(io.gatling.mqtt.protocol.MqttProtocolBuilder wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Use MQTT 3.1
   *
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder mqttVersion_3_1() {
    return new MqttProtocolBuilder(wrapped.mqttVersion_3_1());
  }

  /**
   * Use MQTT 3.1.1 (default)
   *
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder mqttVersion_3_1_1() {
    return new MqttProtocolBuilder(wrapped.mqttVersion_3_1_1());
  }

  /**
   * Define the MQTT broker address
   *
   * @param hostname the hostname
   * @param port the port
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder broker(@Nonnull String hostname, int port) {
    return new MqttProtocolBuilder(wrapped.broker(hostname, port));
  }

  /**
   * Use TLS
   *
   * @param useTls true to enable TLS
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder useTls(boolean useTls) {
    return new MqttProtocolBuilder(wrapped.useTls(useTls));
  }

  /**
   * Provide a function to set a {@link KeyManagerFactory} per virtual user
   *
   * @param f the function to feed a {@link KeyManagerFactory} per virtual user. Input is the
   *     virtual user's unique id.
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder perUserKeyManagerFactory(
      @Nonnull Function<Long, KeyManagerFactory> f) {
    return new MqttProtocolBuilder(
        wrapped.perUserKeyManagerFactory(untyped -> f.apply((Long) untyped)));
  }

  /**
   * Define the clientId
   *
   * @param clientId the clientId, expressed as a Gatling Expression Language String
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder clientId(@Nonnull String clientId) {
    return new MqttProtocolBuilder(wrapped.clientId(toStringExpression(clientId)));
  }

  /**
   * Define the clientId
   *
   * @param clientId the clientId, expressed as a function
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder clientId(@Nonnull Function<Session, String> clientId) {
    return new MqttProtocolBuilder(wrapped.clientId(javaFunctionToExpression(clientId)));
  }

  /**
   * Clean the MQTT session when closing the MQTT connection
   *
   * @param cleanSession true to clean the session
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder cleanSession(boolean cleanSession) {
    return new MqttProtocolBuilder(wrapped.cleanSession(cleanSession));
  }

  /**
   * Define the connect timeout
   *
   * @param timeout the timeout in seconds
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder connectTimeout(long timeout) {
    return connectTimeout(Duration.ofSeconds(timeout));
  }

  /**
   * Define the connect timeout
   *
   * @param timeout the timeout
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder connectTimeout(@Nonnull Duration timeout) {
    return new MqttProtocolBuilder(wrapped.connectTimeout(toScalaDuration(timeout)));
  }

  /**
   * Define the keepAlive timeout
   *
   * @param timeout the keepAlive timeout in seconds
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder keepAlive(long timeout) {
    return keepAlive(Duration.ofSeconds(timeout));
  }

  /**
   * Define the keepAlive timeout
   *
   * @param timeout the keepAlive timeout
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder keepAlive(@Nonnull Duration timeout) {
    return new MqttProtocolBuilder(wrapped.keepAlive(toScalaDuration(timeout)));
  }

  /**
   * Use an at-most-once QoS
   *
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder qosAtMostOnce() {
    return new MqttProtocolBuilder(wrapped.qosAtMostOnce());
  }

  /**
   * Use an at-least-once QoS
   *
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder qosAtLeastOnce() {
    return new MqttProtocolBuilder(wrapped.qosAtLeastOnce());
  }

  /**
   * Use an exactly-once QoS
   *
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder qosExactlyOnce() {
    return new MqttProtocolBuilder(wrapped.qosExactlyOnce());
  }

  /**
   * Instruct the server to retain the message
   *
   * @param retain true to retain
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder retain(boolean retain) {
    return new MqttProtocolBuilder(wrapped.retain(retain));
  }

  /**
   * Define the credentials
   *
   * @param userName the username, expressed as a Gatling Expression Language String
   * @param password the password, expressed as a Gatling Expression Language String
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder credentials(@Nonnull String userName, @Nonnull String password) {
    return new MqttProtocolBuilder(
        wrapped.credentials(toStringExpression(userName), toStringExpression(password)));
  }

  /**
   * Define the credentials
   *
   * @param userName the username, expressed as a function
   * @param password the password, expressed as a Gatling Expression Language String
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder credentials(
      @Nonnull Function<Session, String> userName, @Nonnull String password) {
    return new MqttProtocolBuilder(
        wrapped.credentials(javaFunctionToExpression(userName), toStringExpression(password)));
  }

  /**
   * Define the credentials
   *
   * @param userName the username, expressed as a Gatling Expression Language String
   * @param password the password, expressed as a function
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder credentials(
      @Nonnull String userName, @Nonnull Function<Session, String> password) {
    return new MqttProtocolBuilder(
        wrapped.credentials(toStringExpression(userName), javaFunctionToExpression(password)));
  }

  /**
   * Define the credentials
   *
   * @param userName the username, expressed as a function
   * @param password the password, expressed as a function
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder credentials(
      @Nonnull Function<Session, String> userName, @Nonnull Function<Session, String> password) {
    return new MqttProtocolBuilder(
        wrapped.credentials(
            javaFunctionToExpression(userName), javaFunctionToExpression(password)));
  }

  /**
   * Send a LastWill message when closing the connetion
   *
   * @param lw the last will message
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder lastWill(@Nonnull LastWillBuilder lw) {
    return new MqttProtocolBuilder(wrapped.lastWill(lw.asScala()));
  }

  /**
   * Define the maximum number of reconnections
   *
   * @param reconnectAttemptsMax the maximum number of reconnections
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder reconnectAttemptsMax(int reconnectAttemptsMax) {
    return new MqttProtocolBuilder(wrapped.reconnectAttemptsMax(reconnectAttemptsMax));
  }

  /**
   * Define the delay before reconnecting a crashed connection
   *
   * @param delay the delay in seconds
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder reconnectDelay(long delay) {
    return reconnectDelay(Duration.ofSeconds(delay));
  }

  /**
   * Define the delay before reconnecting a crashed connection
   *
   * @param delay the delay
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder reconnectDelay(@Nonnull Duration delay) {
    return new MqttProtocolBuilder(wrapped.reconnectDelay(toScalaDuration(delay)));
  }

  /**
   * Define the reconnect delay exponential backoff multiplier
   *
   * @param multiplier the multiplier
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder reconnectBackoffMultiplier(float multiplier) {
    return new MqttProtocolBuilder(wrapped.reconnectBackoffMultiplier(multiplier));
  }

  /**
   * Define the delay before resending a message
   *
   * @param delay the delay in seconds
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder resendDelay(long delay) {
    return resendDelay(Duration.ofSeconds(delay));
  }

  /**
   * Define the delay before resending a message
   *
   * @param delay the delay
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder resendDelay(@Nonnull Duration delay) {
    return new MqttProtocolBuilder(wrapped.resendDelay(toScalaDuration(delay)));
  }

  /**
   * Define the resend delay exponential backoff multiplier
   *
   * @param multiplier the multiplier
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder resendBackoffMultiplier(float multiplier) {
    return new MqttProtocolBuilder(wrapped.resendBackoffMultiplier(multiplier));
  }

  /**
   * Define a check to extract the correlationId when applying check that have to match outbound and
   * inbound messages
   *
   * @param correlator the check to extract the correlationId
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder correlateBy(@Nonnull CheckBuilder correlator) {
    return new MqttProtocolBuilder(
        wrapped.correlateBy(MessageCorrelators.toScalaCorrelator(correlator)));
  }

  /**
   * Define the interval to check for checks timeout
   *
   * @param interval the interval in seconds
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder timeoutCheckInterval(long interval) {
    return timeoutCheckInterval(Duration.ofSeconds(interval));
  }

  /**
   * Define the interval to check for checks timeout
   *
   * @param interval the interval
   * @return a new MqttProtocolBuilder instance
   */
  @Nonnull
  public MqttProtocolBuilder timeoutCheckInterval(@Nonnull Duration interval) {
    return new MqttProtocolBuilder(wrapped.timeoutCheckInterval(toScalaDuration(interval)));
  }

  @Override
  public Protocol protocol() {
    return wrapped.build();
  }
}
