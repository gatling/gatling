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

package io.gatling.javaapi.jms;

import static io.gatling.javaapi.core.internal.Converters.*;
import static io.gatling.javaapi.core.internal.Expressions.*;

import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.internal.Converters;
import io.gatling.javaapi.jms.internal.JmsChecks;
import io.gatling.javaapi.jms.internal.ScalaJmsRequestReplyActionBuilderConditions;
import io.gatling.jms.request.RequestReplyDslBuilder;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.jms.Destination;

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
  @Nonnull
  public JmsRequestReplyActionBuilder property(@Nonnull String key, @Nonnull String value) {
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
  @Nonnull
  public JmsRequestReplyActionBuilder property(
      @Nonnull Function<Session, String> key, @Nonnull String value) {
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
  @Nonnull
  public JmsRequestReplyActionBuilder property(
      @Nonnull String key, @Nonnull Function<Session, Object> value) {
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
  @Nonnull
  public JmsRequestReplyActionBuilder property(
      @Nonnull Function<Session, String> key, @Nonnull Function<Session, Object> value) {
    return new JmsRequestReplyActionBuilder(
        wrapped.property(javaFunctionToExpression(key), javaFunctionToExpression(value)));
  }

  /**
   * Set the JMS type
   *
   * @param jmsType the JMS type value, expressed as a Gatling Expression Language String
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @Nonnull
  public JmsRequestReplyActionBuilder jmsType(@Nonnull String jmsType) {
    return new JmsRequestReplyActionBuilder(wrapped.jmsType(toStringExpression(jmsType)));
  }

  /**
   * Set the JMS type
   *
   * @param jmsType the JMS type value, expressed as a function
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @Nonnull
  public JmsRequestReplyActionBuilder jmsType(@Nonnull Function<Session, String> jmsType) {
    return new JmsRequestReplyActionBuilder(wrapped.jmsType(javaFunctionToExpression(jmsType)));
  }

  /**
   * Define some checks
   *
   * @param checks the checks
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @Nonnull
  public JmsRequestReplyActionBuilder check(@Nonnull CheckBuilder... checks) {
    return check(Arrays.asList(checks));
  }

  /**
   * Define some checks
   *
   * @param checks the checks
   * @return a new JmsRequestReplyActionBuilder instance
   */
  @Nonnull
  public JmsRequestReplyActionBuilder check(@Nonnull List<CheckBuilder> checks) {
    return new JmsRequestReplyActionBuilder(wrapped.check(JmsChecks.toScalaChecks(checks)));
  }

  /**
   * Define some checks to be applied only if some condition holds true
   *
   * @param condition the condition, expressed as a Gatling Expression Language String
   * @return the next DSL step
   */
  @Nonnull
  public UntypedCondition checkIf(@Nonnull String condition) {
    return new UntypedCondition(
        ScalaJmsRequestReplyActionBuilderConditions.untyped(wrapped, condition));
  }

  /**
   * Define some checks to be applied only if some condition holds true
   *
   * @param condition the condition, expressed as a function
   * @return the next DSL step
   */
  @Nonnull
  public UntypedCondition checkIf(@Nonnull Function<Session, Boolean> condition) {
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
    @Nonnull
    public JmsRequestReplyActionBuilder then(@Nonnull CheckBuilder... checks) {
      return then(Arrays.asList(checks));
    }

    /**
     * Define the checks to apply if the condition holds true
     *
     * @param checks the checks
     * @return a new JmsRequestReplyActionBuilder instance
     */
    @Nonnull
    public JmsRequestReplyActionBuilder then(@Nonnull List<CheckBuilder> checks) {
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
  @Nonnull
  public TypedCondition checkIf(
      @Nonnull BiFunction<javax.jms.Message, Session, Boolean> condition) {
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
    @Nonnull
    public JmsRequestReplyActionBuilder then(@Nonnull CheckBuilder... checks) {
      return then(Arrays.asList(checks));
    }

    /**
     * Define the checks to apply if the condition holds true
     *
     * @param checks the checks
     * @return a new JmsRequestReplyActionBuilder instance
     */
    @Nonnull
    public JmsRequestReplyActionBuilder then(@Nonnull List<CheckBuilder> checks) {
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
    @Nonnull
    public JmsRequestReplyActionBuilder.Message queue(@Nonnull String name) {
      return new JmsRequestReplyActionBuilder.Message(wrapped.queue(toStringExpression(name)));
    }

    /**
     * Define the destination as a queue
     *
     * @param name the name of the queue, expressed as a function
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder.Message queue(@Nonnull Function<Session, String> name) {
      return new JmsRequestReplyActionBuilder.Message(
          wrapped.queue(javaFunctionToExpression(name)));
    }

    /**
     * Define the queue name as a destination
     *
     * @param destination the destination
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder.Message destination(@Nonnull JmsDestination destination) {
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
    @Nonnull
    public Message replyQueue(@Nonnull String name) {
      return new Message(wrapped.replyQueue(toStringExpression(name)));
    }

    /**
     * Define the reply queue
     *
     * @param name the queue name, expressed as a function
     * @return the next DSL step
     */
    @Nonnull
    public Message replyQueue(@Nonnull Function<Session, String> name) {
      return new Message(wrapped.replyQueue(javaFunctionToExpression(name)));
    }

    /**
     * Define the reply destination
     *
     * @param destination the destination
     * @return the next DSL step
     */
    @Nonnull
    public Message replyDestination(@Nonnull JmsDestination destination) {
      return new Message(wrapped.replyDestination(destination.asScala()));
    }

    /**
     * Don't set the ReplyTo destination, see {@link javax.jms.Message#setJMSReplyTo(Destination)}
     *
     * @return the next DSL step
     */
    @Nonnull
    public Message noJmsReplyTo() {
      return new Message(wrapped.noJmsReplyTo());
    }

    /**
     * Set a response tracking queue that's different from the replyTo queue
     *
     * @param name the queue name, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @Nonnull
    public Message trackerQueue(@Nonnull String name) {
      return new Message(wrapped.trackerQueue(toStringExpression(name)));
    }

    /**
     * Set a response tracking queue that's different from the replyTo queue
     *
     * @param name the queue name, expressed as a function
     * @return the next DSL step
     */
    @Nonnull
    public Message trackerQueue(@Nonnull Function<Session, String> name) {
      return new Message(wrapped.trackerQueue(javaFunctionToExpression(name)));
    }

    /**
     * Set a response tracking destination that's different from the replyTo one
     *
     * @param destination the destination
     * @return the next DSL step
     */
    @Nonnull
    public Message trackerDestination(@Nonnull JmsDestination destination) {
      return new Message(wrapped.trackerDestination(destination.asScala()));
    }

    /**
     * Define a JMS selector, see {@link javax.jms.Session#createConsumer(Destination, String)}
     *
     * @param select the message selector, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @Nonnull
    public Message selector(@Nonnull String select) {
      return new Message(wrapped.selector(toStringExpression(select)));
    }

    /**
     * Define a JMS selector, see {@link javax.jms.Session#createConsumer(Destination, String)}
     *
     * @param select the message selector, expressed as a function
     * @return the next DSL step
     */
    @Nonnull
    public Message selector(@Nonnull Function<Session, String> select) {
      return new Message(wrapped.selector(javaFunctionToExpression(select)));
    }

    /**
     * Send a text message
     *
     * @param msg the message, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder textMessage(@Nonnull String msg) {
      return new JmsRequestReplyActionBuilder(wrapped.textMessage(toStringExpression(msg)));
    }

    /**
     * Send a text message
     *
     * @param msg the message, expressed as a function
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder textMessage(@Nonnull Function<Session, String> msg) {
      return new JmsRequestReplyActionBuilder(wrapped.textMessage(javaFunctionToExpression(msg)));
    }

    /**
     * Send a binary message
     *
     * @param msg the static message
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder bytesMessage(@Nonnull byte[] msg) {
      return new JmsRequestReplyActionBuilder(wrapped.bytesMessage(toStaticValueExpression(msg)));
    }

    /**
     * Send a binary message
     *
     * @param msg the message, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder bytesMessage(@Nonnull String msg) {
      return new JmsRequestReplyActionBuilder(wrapped.bytesMessage(toBytesExpression(msg)));
    }

    /**
     * Send a binary message
     *
     * @param msg the message, expressed as a function
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder bytesMessage(@Nonnull Function<Session, byte[]> msg) {
      return new JmsRequestReplyActionBuilder(wrapped.bytesMessage(javaFunctionToExpression(msg)));
    }

    /**
     * Send a Map message
     *
     * @param msg the static message
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder mapMessage(@Nonnull Map<String, Object> msg) {
      return new JmsRequestReplyActionBuilder(
          wrapped.mapMessage(toStaticValueExpression(toScalaMap(msg))));
    }

    /**
     * Send a Map message
     *
     * @param msg the message, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder mapMessage(@Nonnull String msg) {
      return new JmsRequestReplyActionBuilder(wrapped.mapMessage(toMapExpression(msg)));
    }

    /**
     * Send a Map message
     *
     * @param msg the message, expressed as a function
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder mapMessage(
        @Nonnull Function<Session, Map<String, Object>> msg) {
      return new JmsRequestReplyActionBuilder(
          wrapped.mapMessage(javaFunctionToExpression(msg.andThen(Converters::toScalaMap))));
    }

    /**
     * Send a Serializable message
     *
     * @param msg the static message
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder objectMessage(@Nonnull Serializable msg) {
      return new JmsRequestReplyActionBuilder(wrapped.objectMessage(toStaticValueExpression(msg)));
    }

    /**
     * Send a Serializable message
     *
     * @param msg the message, expressed as a Gatling Expression Language String
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder objectMessage(@Nonnull String msg) {
      return new JmsRequestReplyActionBuilder(
          wrapped.objectMessage(toExpression(msg, Serializable.class)));
    }

    /**
     * Send a Serializable message
     *
     * @param msg the message, expressed as a function
     * @return the next DSL step
     */
    @Nonnull
    public JmsRequestReplyActionBuilder objectMessage(
        @Nonnull Function<Session, Serializable> msg) {
      return new JmsRequestReplyActionBuilder(wrapped.objectMessage(javaFunctionToExpression(msg)));
    }
  }
}
