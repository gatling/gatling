/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import edu.umd.cs.findbugs.annotations.NonNull;

public final class LastWillBuilder {

  private final io.gatling.mqtt.protocol.LastWillBuilder wrapped;

  LastWillBuilder(io.gatling.mqtt.protocol.LastWillBuilder wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * Use an at-most-once QoS
   *
   * @return a new LastWillBuilder instance
   */
  @NonNull
  public LastWillBuilder qosAtMostOnce() {
    return new LastWillBuilder(wrapped.qosAtMostOnce());
  }

  /**
   * Use an at-least-once QoS
   *
   * @return a new LastWillBuilder instance
   */
  @NonNull
  public LastWillBuilder qosAtLeastOnce() {
    return new LastWillBuilder(wrapped.qosAtLeastOnce());
  }

  /**
   * Use an exactly-once QoS
   *
   * @return a new LastWillBuilder instance
   */
  @NonNull
  public LastWillBuilder qosExactlyOnce() {
    return new LastWillBuilder(wrapped.qosExactlyOnce());
  }

  /**
   * Instruct the server to retain the last will message
   *
   * @return a new LastWillBuilder instance
   */
  @NonNull
  public LastWillBuilder retain(boolean newRetain) {
    return new LastWillBuilder(wrapped.retain(newRetain));
  }

  public io.gatling.mqtt.protocol.LastWillBuilder asScala() {
    return wrapped;
  }
}
