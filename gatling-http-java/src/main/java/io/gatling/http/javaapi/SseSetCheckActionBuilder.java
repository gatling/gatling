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
import io.gatling.http.action.sse.SseSetCheckBuilder;

import java.util.function.Function;

public class SseSetCheckActionBuilder implements SseAwaitActionBuilder<SseSetCheckActionBuilder, io.gatling.http.action.sse.SseSetCheckBuilder> {

  private final io.gatling.http.action.sse.SseSetCheckBuilder wrapped;

  public SseSetCheckActionBuilder(io.gatling.http.action.sse.SseSetCheckBuilder wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public SseSetCheckActionBuilder make(Function<SseSetCheckBuilder, SseSetCheckBuilder> f) {
    return new SseSetCheckActionBuilder(f.apply(wrapped));
  }

  @Override
  public Action build(ScenarioContext ctx, Action next) {
    return wrapped.build(ctx, next);
  }
}
