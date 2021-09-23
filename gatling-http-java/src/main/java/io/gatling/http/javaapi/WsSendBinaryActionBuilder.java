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

package io.gatling.http.javaapi;

import io.gatling.core.action.Action;
import io.gatling.core.structure.ScenarioContext;
import io.gatling.http.action.ws.WsSendBinaryFrameBuilder;
import io.gatling.http.check.ws.WsFrameCheck;

import java.util.function.Function;

public class WsSendBinaryActionBuilder implements WsAwaitActionBuilder<WsSendBinaryActionBuilder, WsSendBinaryFrameBuilder, WsFrameCheck.Binary> {

  private final WsSendBinaryFrameBuilder wrapped;

  public WsSendBinaryActionBuilder(WsSendBinaryFrameBuilder wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public WsSendBinaryActionBuilder make(Function<WsSendBinaryFrameBuilder, WsSendBinaryFrameBuilder> f) {
    return new WsSendBinaryActionBuilder(f.apply(wrapped));
  }

  @Override
  public Action build(ScenarioContext ctx, Action next) {
    return wrapped.build(ctx, next);
  }
}
