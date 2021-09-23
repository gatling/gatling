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

package io.gatling.core.javaapi.internal.feed;

import io.gatling.core.feeder.FeederBuilderBase;
import io.gatling.core.javaapi.internal.StructureBuilder;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.gatling.core.javaapi.internal.ScalaHelpers.toScalaFeeder;

public interface Feeds<T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  default T feed(Supplier<Map<String, Object>> feeder) {
    return make(wrapped -> wrapped.feed(toScalaFeeder(feeder)));
  }

  default T feed(Iterator<Map<String, Object>> feeder) {
    return make(wrapped -> wrapped.feed(toScalaFeeder(feeder)));
  }

  default T feed(FeederBuilderBase<?> feederBuilder) {
    return make(wrapped -> wrapped.feed(feederBuilder));
  }
}
