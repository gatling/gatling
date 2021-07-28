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

package io.gatling.jms.javaapi;

import static io.gatling.core.javaapi.internal.Converters.*;
import static io.gatling.core.javaapi.internal.Expressions.*;

import io.gatling.core.check.Check;
import io.gatling.core.check.CheckMaterializer;
import io.gatling.core.javaapi.CheckBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.jms.javaapi.internal.JmsCheckType;
import java.util.function.Function;
import javax.jms.Message;

public final class Predef {
  private Predef() {}

  /** Prefix to bootstrap a new JMS protocol builder DSL */
  public static JmsProtocolBuilder.Base jms() {
    return JmsProtocolBuilder.Base.INSTANCE;
  }

  /** Bootstrap a JmsJndiConnectionFactoryBuilder DSL */
  public static JmsJndiConnectionFactoryBuilder.Base jmsJndiConnectionFactory =
      JmsJndiConnectionFactoryBuilder.Base.INSTANCE;

  /**
   * Bootstrap a JMS action builder
   *
   * @param name the name of the action, expressed as a Gatling Expression Language String
   * @return a new Jms instance
   */
  public static Jms jms(String name) {
    return new Jms(toStringExpression(name));
  }

  /**
   * Bootstrap a JMS action builder
   *
   * @param name the name of the action, expressed as a function
   * @return a new Jms instance
   */
  public static Jms jms(Function<Session, String> name) {
    return new Jms(javaFunctionToExpression(name));
  }

  /**
   * Create a new JMS topic
   *
   * @param name the name of the topic, expressed as a Gatling Expression Language String
   * @return a new JmsDestination
   */
  public static JmsDestination topic(String name) {
    return JmsDestination.topic(toStringExpression(name));
  }

  /**
   * Create a new JMS topic
   *
   * @param name the name of the topic, expressed as a function
   * @return a new JmsDestination
   */
  public static JmsDestination topic(Function<Session, String> name) {
    return JmsDestination.topic(javaFunctionToExpression(name));
  }

  /**
   * Create a new JMS queue
   *
   * @param name the name of the queue, expressed as a Gatling Expression Language String
   * @return a new JmsDestination
   */
  public static JmsDestination queue(String name) {
    return JmsDestination.queue(toStringExpression(name));
  }

  /**
   * Create a new JMS queue
   *
   * @param name the name of the queue, expressed as a function
   * @return a new JmsDestination
   */
  public static JmsDestination queue(Function<Session, String> name) {
    return JmsDestination.queue(javaFunctionToExpression(name));
  }

  /**
   * Create a simple JMS check from a function
   *
   * @param f the function, returns true if the message is valid
   * @return a new CheckBuilder
   */
  @SuppressWarnings("rawtypes")
  public static CheckBuilder simpleCheck(Function<Message, Boolean> f) {
    return new CheckBuilder() {
      @Override
      public io.gatling.core.check.CheckBuilder<?, ?> asScala() {
        return new io.gatling.core.check.CheckBuilder() {
          @Override
          public Check<?> build(CheckMaterializer materializer) {
            return io.gatling.jms.Predef.simpleCheck(
                toScalaFunction(f.andThen(Function.identity()))); // trick compiler
          }
        };
      }

      @Override
      public CheckType type() {
        return JmsCheckType.Simple;
      }
    };
  }
}
