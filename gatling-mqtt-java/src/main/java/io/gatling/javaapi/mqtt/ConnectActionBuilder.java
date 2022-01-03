/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import io.gatling.javaapi.core.ActionBuilder;

/** The builder for connect action */
public final class ConnectActionBuilder implements ActionBuilder {

  private final io.gatling.mqtt.action.builder.ConnectBuilder wrapped;

  ConnectActionBuilder(io.gatling.mqtt.action.builder.ConnectBuilder wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public io.gatling.core.action.builder.ActionBuilder asScala() {
    return wrapped;
  }
}
