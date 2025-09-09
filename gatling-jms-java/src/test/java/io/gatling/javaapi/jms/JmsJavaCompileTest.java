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

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.jms.JmsDsl.*;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import java.util.Map;
import org.jspecify.annotations.NonNull;

public class JmsJavaCompileTest extends Simulation {

  private static final JmsMessageMatcher HEADER_MATCHER =
      new JmsMessageMatcher() {
        @Override
        public void prepareRequest(@NonNull Message msg) {}

        @Override
        public @NonNull String requestMatchId(@NonNull Message msg) throws JMSException {
          return msg.getStringProperty("header");
        }

        @Override
        public @NonNull String responseMatchId(@NonNull Message msg) throws JMSException {
          return requestMatchId(msg);
        }
      };

  // create JmsProtocol from standard ConnectionFactory
  private final JmsProtocolBuilder jmsProtocol =
      jms.connectionFactory(
          new org.apache.activemq.ActiveMQConnectionFactory("tcp://localhost:61616"));

  // create JmsProtocol from JNDI based ConnectionFactory
  private JmsProtocolBuilder jmsProtocolWithJndiConnectionFactory =
      jms.connectionFactory(
              jmsJndiConnectionFactory
                  .connectionFactoryName("ConnectionFactory")
                  .url("tcp://localhost:10002")
                  .credentials("user", "secret")
                  .contextFactory("org.apache.activemq.jndi.ActiveMQInitialContextFactory"))
          .usePersistentDeliveryMode()
          .replyTimeout(1)
          .messageMatcher(HEADER_MATCHER)
          .matchByCorrelationId()
          .matchByMessageId();

  private ScenarioBuilder scn =
      scenario("scn")
          // requestReply
          // textMessage
          .exec(
              jms("req")
                  .requestReply()
                  .queue("queue")
                  //        .replyQueue("responseQueue")
                  .textMessage("hello"))
          // textMessage with ElFileBody
          .exec(jms("req").requestReply().queue("queue").textMessage(ElFileBody("file")))
          // bytesMessage
          .exec(jms("req").requestReply().queue("queue").bytesMessage(new byte[] {1}))
          // bytesMessage with RawFileBody
          .exec(jms("req").requestReply().queue("queue").bytesMessage(RawFileBody("file")))
          //    // mapMessage
          .exec(jms("req").requestReply().queue("queue").mapMessage(Map.of("key", "value")))
          // objectMessage
          .exec(jms("req").requestReply().queue("queue").objectMessage("hello"))
          // destination
          .exec(
              jms("req")
                  .requestReply()
                  .destination(topic("topic"))
                  .noJmsReplyTo()
                  .replyDestination(queue("queue"))
                  .trackerQueue("queue")
                  .trackerDestination(topic("topic"))
                  .selector("selector")
                  .textMessage("hello"))
          // check
          .exec(
              jms("req")
                  .requestReply()
                  .queue("queue")
                  .textMessage("hello")
                  .check(
                      simpleCheck(
                          msg -> {
                            if (msg instanceof TextMessage) {
                              try {
                                return ((TextMessage) msg).getText().equals("hello");
                              } catch (JMSException e) {
                                return false;
                              }
                            } else {
                              return false;
                            }
                          }))
                  .check(xpath("//TEST").saveAs("name"))
                  .check(jsonPath("$.foo"))
                  .check(jmesPath("[].foo"))
                  .check(substring("foo"))
                  .check(bodyLength().lte(50))
                  .check(bodyBytes().transform(bytes -> bytes.length).lte(50))
                  .check(bodyString())
                  .check(bodyString().is("hello"), substring("he").count().is(1))
                  .check(jmsProperty("header").is("foo"))
                  .check(jmsProperty("header").ofInt().is(1))
                  .checkIf("#{bool}")
                  .then(jsonPath("$..foo"))
                  .checkIf((message, session) -> true)
                  .then(jsonPath("$").is("hello")))
          // extra
          .exec(
              jms("req")
                  .requestReply()
                  .queue("queue")
                  .textMessage("hello")
                  .property("header", "value")
                  .property("header", "#{value}")
                  .property("header", session -> "value")
                  .jmsType("foo"))

          // send
          // textMessage
          .exec(jms("req").send().queue("queue").textMessage("hello"))
          // bytesMessage
          .exec(jms("req").send().queue("queue").bytesMessage(new byte[] {1}))
          // objectMessage
          .exec(jms("req").send().queue("queue").objectMessage("hello"))
          // mapMessage
          .exec(jms("req").send().queue("queue").mapMessage(Map.of("key", "value")))
          // destination: topic
          .exec(jms("req").send().destination(topic("topic")).textMessage("hello"))
          // destination: queue
          .exec(jms("req").send().destination(queue("queue")).textMessage("hello"))
          // extra
          .exec(
              jms("req")
                  .send()
                  .queue("queue")
                  .textMessage("hello")
                  .property("header", "value")
                  .property("header", "#{value}")
                  .property("header", session -> "value")
                  .jmsType("foo"));

  {
    setUp(scn.injectOpen(atOnceUsers(1))).protocols(jmsProtocol);
  }
}
