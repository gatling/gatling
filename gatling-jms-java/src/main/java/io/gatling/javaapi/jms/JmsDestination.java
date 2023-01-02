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

import io.gatling.commons.validation.Validation;
import io.gatling.core.session.Session;
import scala.Function1;

public abstract class JmsDestination {

  /** Configuration for using a temporary queue */
  public static final JmsDestination TemporaryQueue =
      new JmsDestination() {
        @Override
        public io.gatling.jms.request.JmsDestination asScala() {
          return io.gatling.jms.request.JmsDestination.TemporaryQueue$.MODULE$;
        }
      };

  /** Configuration for using a temporary topic */
  public static final JmsDestination TemporaryTopic =
      new JmsDestination() {
        @Override
        public io.gatling.jms.request.JmsDestination asScala() {
          return io.gatling.jms.request.JmsDestination.TemporaryTopic$.MODULE$;
        }
      };

  static JmsDestination queue(Function1<Session, Validation<String>> name) {
    return new JmsDestination() {
      @Override
      public io.gatling.jms.request.JmsDestination asScala() {
        return new io.gatling.jms.request.JmsDestination.Queue(name);
      }
    };
  }

  static JmsDestination topic(Function1<Session, Validation<String>> name) {
    return new JmsDestination() {
      @Override
      public io.gatling.jms.request.JmsDestination asScala() {
        return new io.gatling.jms.request.JmsDestination.Topic(name);
      }
    };
  }

  private JmsDestination() {}

  public abstract io.gatling.jms.request.JmsDestination asScala();
}
