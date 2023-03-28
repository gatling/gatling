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

package io.gatling.javaapi.jms;

import static io.gatling.javaapi.core.internal.Converters.*;
import static io.gatling.javaapi.core.internal.Expressions.*;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.internal.Converters;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;

/**
 * DSL for building send/fire-and-forget actions.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class JmsSendActionBuilder implements ActionBuilder {

  private final io.gatling.jms.request.SendDslBuilder wrapped;

  public JmsSendActionBuilder(io.gatling.jms.request.SendDslBuilder wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Set a property
   *
   * @param key the property key, expressed as a Gatling Expression Language String
   * @param value the property value, expressed as a Gatling Expression Language String
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @NonNull
  public JmsSendActionBuilder property(@NonNull String key, @NonNull String value) {
    return new JmsSendActionBuilder(
        wrapped.property(toStringExpression(key), toAnyExpression(value)));
  }

  /**
   * Set a property
   *
   * @param key the property key, expressed as a function
   * @param value the property value, expressed as a Gatling Expression Language String
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @NonNull
  public JmsSendActionBuilder property(
      @NonNull Function<Session, String> key, @NonNull String value) {
    return new JmsSendActionBuilder(
        wrapped.property(javaFunctionToExpression(key), toAnyExpression(value)));
  }

  /**
   * Set a property
   *
   * @param key the property key, expressed as a Gatling Expression Language String
   * @param value the property value, expressed as a function
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @NonNull
  public JmsSendActionBuilder property(
      @NonNull String key, @NonNull Function<Session, Object> value) {
    return new JmsSendActionBuilder(
        wrapped.property(toStringExpression(key), javaFunctionToExpression(value)));
  }

  /**
   * Set a property
   *
   * @param key the property key, expressed as a function
   * @param value the property value, expressed as a function
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @NonNull
  public JmsSendActionBuilder property(
      @NonNull Function<Session, String> key, @NonNull Function<Session, Object> value) {
    return new JmsSendActionBuilder(
        wrapped.property(javaFunctionToExpression(key), javaFunctionToExpression(value)));
  }

  /**
   * Set the JMS type
   *
   * @param jmsType the JMS type value, expressed as a Gatling Expression Language String
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @NonNull
  public JmsSendActionBuilder jmsType(@NonNull String jmsType) {
    return new JmsSendActionBuilder(wrapped.jmsType(toStringExpression(jmsType)));
  }

  /**
   * Set the JMS type
   *
   * @param jmsType the JMS type value, expressed as a function
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @NonNull
  public JmsSendActionBuilder jmsType(@NonNull Function<Session, String> jmsType) {
    return new JmsSendActionBuilder(wrapped.jmsType(javaFunctionToExpression(jmsType)));
  }

  @Override
  public io.gatling.core.action.builder.ActionBuilder asScala() {
    return wrapped.build();
  }

  public static final class Queue {
    private final io.gatling.jms.request.SendDslBuilder.Queue wrapped;

    public Queue(io.gatling.jms.request.SendDslBuilder.Queue wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the destination as a queue
     *
     * @param name the name of the queue, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public Message queue(@NonNull String name) {
      return new Message(wrapped.queue(toStringExpression(name)));
    }

    /**
     * Define the destination as a queue
     *
     * @param name the name of the queue, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public Message queue(@NonNull Function<Session, String> name) {
      return new Message(wrapped.queue(javaFunctionToExpression(name)));
    }

    /**
     * Define the queue name as a destination
     *
     * @param destination the destination
     * @return the next DSL step
     */
    @NonNull
    public Message destination(@NonNull JmsDestination destination) {
      return new Message(wrapped.destination(destination.asScala()));
    }
  }

  public static final class Message {
    private final io.gatling.jms.request.SendDslBuilder.Message wrapped;

    public Message(io.gatling.jms.request.SendDslBuilder.Message wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Send a text message
     *
     * @param msg the message, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public JmsSendActionBuilder textMessage(@NonNull String msg) {
      return new JmsSendActionBuilder(wrapped.textMessage(toStringExpression(msg)));
    }

    /**
     * Send a text message
     *
     * @param msg the message, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public JmsSendActionBuilder textMessage(@NonNull Function<Session, String> msg) {
      return new JmsSendActionBuilder(wrapped.textMessage(javaFunctionToExpression(msg)));
    }

    /**
     * Send a binary message
     *
     * @param msg the static message
     * @return the next DSL step
     */
    @NonNull
    public JmsSendActionBuilder bytesMessage(@NonNull byte[] msg) {
      return new JmsSendActionBuilder(wrapped.bytesMessage(toStaticValueExpression(msg)));
    }

    /**
     * Send a binary message
     *
     * @param msg the message, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public JmsSendActionBuilder bytesMessage(@NonNull String msg) {
      return new JmsSendActionBuilder(wrapped.bytesMessage(toBytesExpression(msg)));
    }

    /**
     * Send a binary message
     *
     * @param msg the message, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public JmsSendActionBuilder bytesMessage(@NonNull Function<Session, byte[]> msg) {
      return new JmsSendActionBuilder(wrapped.bytesMessage(javaFunctionToExpression(msg)));
    }

    /**
     * Send a Map message
     *
     * @param msg the static message
     * @return the next DSL step
     */
    @NonNull
    public JmsSendActionBuilder mapMessage(@NonNull Map<String, Object> msg) {
      return new JmsSendActionBuilder(wrapped.mapMessage(toStaticValueExpression(toScalaMap(msg))));
    }

    /**
     * Send a Map message
     *
     * @param msg the message, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public JmsSendActionBuilder mapMessage(@NonNull String msg) {
      return new JmsSendActionBuilder(wrapped.mapMessage(toMapExpression(msg)));
    }

    /**
     * Send a Map message
     *
     * @param msg the message, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public JmsSendActionBuilder mapMessage(@NonNull Function<Session, Map<String, Object>> msg) {
      return new JmsSendActionBuilder(
          wrapped.mapMessage(javaFunctionToExpression(msg.andThen(Converters::toScalaMap))));
    }

    /**
     * Send a Serializable message
     *
     * @param msg the static message
     * @return the next DSL step
     */
    @NonNull
    public JmsSendActionBuilder objectMessage(@NonNull Serializable msg) {
      return new JmsSendActionBuilder(wrapped.objectMessage(toStaticValueExpression(msg)));
    }

    /**
     * Send a Serializable message
     *
     * @param msg the message, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public JmsSendActionBuilder objectMessage(@NonNull String msg) {
      return new JmsSendActionBuilder(wrapped.objectMessage(toExpression(msg, Serializable.class)));
    }

    /**
     * Send a Serializable message
     *
     * @param msg the message, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public JmsSendActionBuilder objectMessage(@NonNull Function<Session, Serializable> msg) {
      return new JmsSendActionBuilder(wrapped.objectMessage(javaFunctionToExpression(msg)));
    }
  }
}
