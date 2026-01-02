/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.jms;

import static io.gatling.javaapi.core.internal.Converters.toScalaDuration;

import io.gatling.core.protocol.Protocol;
import io.gatling.javaapi.core.ProtocolBuilder;
import io.gatling.javaapi.jms.internal.JmsMessageMatchers;
import jakarta.jms.ConnectionFactory;
import java.time.Duration;
import org.jspecify.annotations.NonNull;

/**
 * DSL for building <a href="https://en.wikipedia.org/wiki/Jakarta_Messaging">JMS</a> Protocol
 * configurations
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class JmsProtocolBuilder implements ProtocolBuilder {

  public static final class Base {

    public static final Base INSTANCE = new Base();

    private Base() {}

    /**
     * Define a programmatic ConnectionFactory
     *
     * @param cf the ConnectionFactory
     * @return a new DSL instance
     */
    public @NonNull JmsProtocolBuilder connectionFactory(@NonNull ConnectionFactory cf) {
      return new JmsProtocolBuilder(
          io.gatling.jms.protocol.JmsProtocolBuilderBase.connectionFactory(cf));
    }

    /**
     * Define a JNDI ConnectionFactory
     *
     * @param cfb the ConnectionFactory
     * @return a new DSL instance
     */
    public @NonNull JmsProtocolBuilder connectionFactory(
        @NonNull JmsJndiConnectionFactoryBuilder cfb) {
      return new JmsProtocolBuilder(
          io.gatling.jms.protocol.JmsProtocolBuilderBase.connectionFactory(cfb.build()));
    }
  }

  private final io.gatling.jms.protocol.JmsProtocolBuilder wrapped;

  public JmsProtocolBuilder(io.gatling.jms.protocol.JmsProtocolBuilder wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public Protocol protocol() {
    return wrapped.build();
  }

  /**
   * Define the connection credentials
   *
   * @param user the user
   * @param password the password
   * @return a new instance
   */
  public @NonNull JmsProtocolBuilder credentials(@NonNull String user, @NonNull String password) {
    return new JmsProtocolBuilder(wrapped.credentials(user, password));
  }

  /**
   * Use the persistent delivery mode
   *
   * @return a new instance
   */
  public @NonNull JmsProtocolBuilder usePersistentDeliveryMode() {
    return new JmsProtocolBuilder(wrapped.usePersistentDeliveryMode());
  }

  /**
   * Use the non-persistent delivery mode
   *
   * @return a new instance
   */
  public @NonNull JmsProtocolBuilder useNonPersistentDeliveryMode() {
    return new JmsProtocolBuilder(wrapped.useNonPersistentDeliveryMode());
  }

  /**
   * Match outbound and inbound messages based on messageId
   *
   * @return a new instance
   */
  public @NonNull JmsProtocolBuilder matchByMessageId() {
    return new JmsProtocolBuilder(wrapped.matchByMessageId());
  }

  /**
   * Match outbound and inbound messages based on correlationId
   *
   * @return a new instance
   */
  public @NonNull JmsProtocolBuilder matchByCorrelationId() {
    return new JmsProtocolBuilder(wrapped.matchByCorrelationId());
  }

  /**
   * Provide a custom message matcher
   *
   * @param matcher the custom message matcher
   * @return a new instance
   */
  public @NonNull JmsProtocolBuilder messageMatcher(@NonNull JmsMessageMatcher matcher) {
    return new JmsProtocolBuilder(wrapped.messageMatcher(JmsMessageMatchers.toScala(matcher)));
  }

  /**
   * Define a reply timeout
   *
   * @param timeout the timeout in seconds
   * @return a new instance
   */
  public @NonNull JmsProtocolBuilder replyTimeout(long timeout) {
    return replyTimeout(Duration.ofSeconds(timeout));
  }

  /**
   * Define a reply timeout
   *
   * @param timeout the timeout
   * @return a new instance
   */
  public @NonNull JmsProtocolBuilder replyTimeout(Duration timeout) {
    return new JmsProtocolBuilder(wrapped.replyTimeout(toScalaDuration(timeout)));
  }

  /**
   * Define the number of listener threads
   *
   * @param threadCount the number of threads
   * @return a new instance
   */
  public @NonNull JmsProtocolBuilder listenerThreadCount(int threadCount) {
    return new JmsProtocolBuilder(wrapped.listenerThreadCount(threadCount));
  }
}
