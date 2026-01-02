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

import io.gatling.commons.validation.Validation;
import io.gatling.core.session.Session;
import org.jspecify.annotations.NonNull;
import scala.Function1;

public final class Jms {
  private final io.gatling.jms.request.JmsDslBuilderBase wrapped;

  public Jms(Function1<Session, Validation<String>> name) {
    wrapped = new io.gatling.jms.request.JmsDslBuilderBase(name);
  }

  /**
   * Bootstrap a builder to send JMS messages
   *
   * @return the next DSL step
   */
  public JmsSendActionBuilder.@NonNull Queue send() {
    return new JmsSendActionBuilder.Queue(wrapped.send());
  }

  /**
   * Bootstrap a builder to create JMS request-reply flows
   *
   * @return the next DSL step
   */
  public JmsRequestReplyActionBuilder.@NonNull Queue requestReply() {
    return new JmsRequestReplyActionBuilder.Queue(wrapped.requestReply());
  }
}
