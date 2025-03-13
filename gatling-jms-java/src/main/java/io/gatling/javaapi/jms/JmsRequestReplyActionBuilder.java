/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.internal.Converters;
import io.gatling.javaapi.jms.internal.JmsChecks;
import io.gatling.javaapi.jms.internal.ScalaJmsRequestReplyActionBuilderConditions;
import io.gatling.jms.request.RequestReplyDslBuilder;
import jakarta.jms.Destination;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * DSL for building request-reply actions.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public class JmsRequestReplyActionBuilder implements ActionBuilder {

  private final io.gatling.jms.request.RequestReplyDslBuilder wrapped;

  public JmsRequestReplyActionBuilder(RequestReplyDslBuilder wrapped) {
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
  public JmsRequestReplyActionBuilder property(@NonNull String key, @NonNull String value) {
    return new JmsRequestReplyActionBuilder(
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
  public JmsRequestReplyActionBuilder property(
      @NonNull Function<Session, String> key, @NonNull String value) {
    return new JmsRequestReplyActionBuilder(
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
  public JmsRequestReplyActionBuilder property(
      @NonNull String key, @NonNull Function<Session, Object> value) {
    return new JmsRequestReplyActionBuilder(
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
  public JmsRequestReplyActionBuilder property(
      @NonNull Function<Session, String> key, @NonNull Function<Session, Object> value) {
    return new JmsRequestReplyActionBuilder(
        wrapped.property(javaFunctionToExpression(key), javaFunctionToExpression(value)));
  }

  /**
   * Set the JMS type
   *
   * @param jmsType the JMS type value, expressed as a Gatling Expression Language String
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @NonNull
  public JmsRequestReplyActionBuilder jmsType(@NonNull String jmsType) {
    return new JmsRequestReplyActionBuilder(wrapped.jmsType(toStringExpression(jmsType)));
  }

  /**
   * Set the JMS type
   *
   * @param jmsType the JMS type value, expressed as a function
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @NonNull
  public JmsRequestReplyActionBuilder jmsType(@NonNull Function<Session, String> jmsType) {
    return new JmsRequestReplyActionBuilder(wrapped.jmsType(javaFunctionToExpression(jmsType)));
  }

  /**
   * Define some checks
   *
   * @param checks the checks
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @NonNull
  public JmsRequestReplyActionBuilder check(@NonNull CheckBuilder... checks) {
    return check(Arrays.asList(checks));
  }

  /**
   * Define some checks
   *
   * @param checks the checks
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @NonNull
  public JmsRequestReplyActionBuilder check(@NonNull List<CheckBuilder> checks) {
    return new JmsRequestReplyActionBuilder(wrapped.check(JmsChecks.toScalaChecks(checks)));
  }

  /**
   * Define some checks to be applied only if some condition holds true
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @NonNull
  public UntypedCondition checkIf(@NonNull String condition) {
    return new UntypedCondition(
        ScalaJmsRequestReplyActionBuilderConditions.untyped(wrapped, condition));
  }

  /**
   * Define some checks to be applied only if some condition holds true
   *
   * @param condition the condition, expressed as a function
   * @return the next DSL step
   */
  @NonNull
  public UntypedCondition checkIf(@NonNull Function<Session, Boolean> condition) {
    return new UntypedCondition(
        ScalaJmsRequestReplyActionBuilderConditions.untyped(wrapped, condition));
  }

  public static final class UntypedCondition {
    private final ScalaJmsRequestReplyActionBuilderConditions.Untyped wrapped;

    public UntypedCondition(ScalaJmsRequestReplyActionBuilderConditions.Untyped wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the checks to apply if the condition holds true
     *
     * @param checks the checks
     * @return a new JmsRequestReplyActionBuilder instance
     */
    @NonNull
    public JmsRequestReplyActionBuilder then(@NonNull CheckBuilder... checks) {
      return then(Arrays.asList(checks));
    }

    /**
     * Define the checks to apply if the condition holds true
     *
     * @param checks the checks
     * @return a new JmsRequestReplyActionBuilder instance
     */
    @NonNull
    public JmsRequestReplyActionBuilder then(@NonNull List<CheckBuilder> checks) {
      return wrapped.thenChecks(checks);
    }
  }

  /**
   * Define some checks to be applied only if some condition holds true
   *
   * @param condition the condition, expressed as a function that's aware of the JMS Message and the
   *     Session
   * @return the next DSL step
   */
  @NonNull
  public TypedCondition checkIf(
      @NonNull BiFunction<jakarta.jms.Message, Session, Boolean> condition) {
    return new TypedCondition(
        ScalaJmsRequestReplyActionBuilderConditions.typed(wrapped, condition));
  }

  public static final class TypedCondition {
    private final ScalaJmsRequestReplyActionBuilderConditions.Typed wrapped;

    public TypedCondition(ScalaJmsRequestReplyActionBuilderConditions.Typed wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the checks to apply if the condition holds true
     *
     * @param checks the checks
     * @return a new JmsRequestReplyActionBuilder instance
     */
    @NonNull
    public JmsRequestReplyActionBuilder then(@NonNull CheckBuilder... checks) {
      return then(Arrays.asList(checks));
    }

    /**
     * Define the checks to apply if the condition holds true
     *
     * @param checks the checks
     * @return a new JmsRequestReplyActionBuilder instance
     */
    @NonNull
    public JmsRequestReplyActionBuilder then(@NonNull List<CheckBuilder> checks) {
      return wrapped.then_(checks);
    }
  }

  @Override
  public io.gatling.core.action.builder.ActionBuilder asScala() {
    return wrapped.build();
  }

  public static final class Queue {
    private final io.gatling.jms.request.RequestReplyDslBuilder.Queue wrapped;

    public Queue(io.gatling.jms.request.RequestReplyDslBuilder.Queue wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the destination as a queue
     *
     * @param name the name of the queue, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder.Message queue(@NonNull String name) {
      return new JmsRequestReplyActionBuilder.Message(wrapped.queue(toStringExpression(name)));
    }

    /**
     * Define the destination as a queue
     *
     * @param name the name of the queue, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder.Message queue(@NonNull Function<Session, String> name) {
      return new JmsRequestReplyActionBuilder.Message(
          wrapped.queue(javaFunctionToExpression(name)));
    }

    /**
     * Define the queue name as a destination
     *
     * @param destination the destination
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder.Message destination(@NonNull JmsDestination destination) {
      return new JmsRequestReplyActionBuilder.Message(wrapped.destination(destination.asScala()));
    }
  }

  public static final class Message {
    private final io.gatling.jms.request.RequestReplyDslBuilder.Message wrapped;

    public Message(io.gatling.jms.request.RequestReplyDslBuilder.Message wrapped) {
      this.wrapped = wrapped;
    }

    /**
     * Define the reply queue
     *
     * @param name the queue name, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public Message replyQueue(@NonNull String name) {
      return new Message(wrapped.replyQueue(toStringExpression(name)));
    }

    /**
     * Define the reply queue
     *
     * @param name the queue name, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public Message replyQueue(@NonNull Function<Session, String> name) {
      return new Message(wrapped.replyQueue(javaFunctionToExpression(name)));
    }

    /**
     * Define the reply destination
     *
     * @param destination the destination
     * @return the next DSL step
     */
    @NonNull
    public Message replyDestination(@NonNull JmsDestination destination) {
      return new Message(wrapped.replyDestination(destination.asScala()));
    }

    /**
     * Don't set the ReplyTo destination, see {@link jakarta.jms.Message#setJMSReplyTo(Destination)}
     *
     * @return the next DSL step
     */
    @NonNull
    public Message noJmsReplyTo() {
      return new Message(wrapped.noJmsReplyTo());
    }

    /**
     * Set a response tracking queue that's different from the replyTo queue
     *
     * @param name the queue name, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public Message trackerQueue(@NonNull String name) {
      return new Message(wrapped.trackerQueue(toStringExpression(name)));
    }

    /**
     * Set a response tracking queue that's different from the replyTo queue
     *
     * @param name the queue name, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public Message trackerQueue(@NonNull Function<Session, String> name) {
      return new Message(wrapped.trackerQueue(javaFunctionToExpression(name)));
    }

    /**
     * Set a response tracking destination that's different from the replyTo one
     *
     * @param destination the destination
     * @return the next DSL step
     */
    @NonNull
    public Message trackerDestination(@NonNull JmsDestination destination) {
      return new Message(wrapped.trackerDestination(destination.asScala()));
    }

    /**
     * Define a JMS selector, see {@link jakarta.jms.Session#createConsumer(Destination, String)}
     *
     * @param select the message selector, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public Message selector(@NonNull String select) {
      return new Message(wrapped.selector(toStringExpression(select)));
    }

    /**
     * Define a JMS selector, see {@link jakarta.jms.Session#createConsumer(Destination, String)}
     *
     * @param select the message selector, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public Message selector(@NonNull Function<Session, String> select) {
      return new Message(wrapped.selector(javaFunctionToExpression(select)));
    }

    /**
     * Send a text message
     *
     * @param msg the message, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder textMessage(@NonNull String msg) {
      return new JmsRequestReplyActionBuilder(wrapped.textMessage(toStringExpression(msg)));
    }

    /**
     * Send a text message
     *
     * @param msg the message, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder textMessage(@NonNull Function<Session, String> msg) {
      return new JmsRequestReplyActionBuilder(wrapped.textMessage(javaFunctionToExpression(msg)));
    }

    /**
     * Send a binary message
     *
     * @param msg the static message
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder bytesMessage(@NonNull byte[] msg) {
      return new JmsRequestReplyActionBuilder(wrapped.bytesMessage(toStaticValueExpression(msg)));
    }

    /**
     * Send a binary message
     *
     * @param msg the message, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder bytesMessage(@NonNull String msg) {
      return new JmsRequestReplyActionBuilder(wrapped.bytesMessage(toBytesExpression(msg)));
    }

    /**
     * Send a binary message
     *
     * @param msg the message, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder bytesMessage(@NonNull Function<Session, byte[]> msg) {
      return new JmsRequestReplyActionBuilder(wrapped.bytesMessage(javaFunctionToExpression(msg)));
    }

    /**
     * Send a Map message
     *
     * @param msg the static message
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder mapMessage(@NonNull Map<String, Object> msg) {
      return new JmsRequestReplyActionBuilder(
          wrapped.mapMessage(toStaticValueExpression(toScalaMap(msg))));
    }

    /**
     * Send a Map message
     *
     * @param msg the message, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder mapMessage(@NonNull String msg) {
      return new JmsRequestReplyActionBuilder(wrapped.mapMessage(toMapExpression(msg)));
    }

    /**
     * Send a Map message
     *
     * @param msg the message, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder mapMessage(
        @NonNull Function<Session, Map<String, Object>> msg) {
      return new JmsRequestReplyActionBuilder(
          wrapped.mapMessage(javaFunctionToExpression(msg.andThen(Converters::toScalaMap))));
    }

    /**
     * Send a Serializable message
     *
     * @param msg the static message
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder objectMessage(@NonNull Serializable msg) {
      return new JmsRequestReplyActionBuilder(wrapped.objectMessage(toStaticValueExpression(msg)));
    }

    /**
     * Send a Serializable message
     *
     * @param msg the message, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder objectMessage(@NonNull String msg) {
      return new JmsRequestReplyActionBuilder(
          wrapped.objectMessage(toExpression(msg, Serializable.class)));
    }

    /**
     * Send a Serializable message
     *
     * @param msg the message, expressed as a function
     * @return the next DSL step
     */
    @NonNull
    public JmsRequestReplyActionBuilder objectMessage(
        @NonNull Function<Session, Serializable> msg) {
      return new JmsRequestReplyActionBuilder(wrapped.objectMessage(javaFunctionToExpression(msg)));
    }
  }
}
