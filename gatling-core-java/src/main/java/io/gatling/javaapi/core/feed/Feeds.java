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

package io.gatling.javaapi.core.feed;

import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.StructureBuilder;
import io.gatling.javaapi.core.internal.feed.ScalaFeeds;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;

/**
 * Feed methods for defining actions to inject data into the virtual users' {@link
 * io.gatling.javaapi.core.Session}.
 *
 * <p>Important: instances are immutable so any method doesn't mutate the existing instance but
 * returns a new one.
 *
 * @param <T> the type of {@link StructureBuilder} to attach to and to return
 * @param <W> the type of wrapped Scala instance
 */
public interface Feeds<
    T extends StructureBuilder<T, W>, W extends io.gatling.core.structure.StructureBuilder<W>> {

  T make(Function<W, W> f);

  /**
   * Attach a feed action
   *
   * @param feederBuilder a supplier so that the underlying {@link Iterator} can be lazily loaded
   * @return a new StructureBuilder
   */
  default @NonNull T feed(@NonNull Supplier<Iterator<Map<String, Object>>> feederBuilder) {
    return ScalaFeeds.apply(this, feederBuilder);
  }

  /**
   * Attach a feed action
   *
   * @param feederBuilder a supplier so that the underlying {@link Iterator} can be lazily loaded
   * @param numberOfRecords the number of records to poll from the feeder at once
   * @return a new StructureBuilder
   */
  default @NonNull T feed(
      @NonNull Supplier<Iterator<Map<String, Object>>> feederBuilder, int numberOfRecords) {
    return ScalaFeeds.apply(this, feederBuilder, numberOfRecords);
  }

  /**
   * Attach a feed action
   *
   * @param feederBuilder a supplier so that the underlying {@link Iterator} can be lazily loaded
   * @param numberOfRecords the number of records to poll from the feeder at once
   * @return a new StructureBuilder
   */
  default @NonNull T feed(
      @NonNull Supplier<Iterator<Map<String, Object>>> feederBuilder, String numberOfRecords) {
    return ScalaFeeds.apply(this, feederBuilder, numberOfRecords);
  }

  /**
   * Attach a feed action
   *
   * @param feederBuilder a supplier so that the underlying {@link Iterator} can be lazily loaded
   * @param numberOfRecords the number of records to poll from the feeder at once
   * @return a new StructureBuilder
   */
  default @NonNull T feed(
      @NonNull Supplier<Iterator<Map<String, Object>>> feederBuilder,
      Function<Session, Integer> numberOfRecords) {
    return ScalaFeeds.apply(this, feederBuilder, numberOfRecords);
  }

  /**
   * Attach a feed action.
   *
   * @param feeder a source of records
   * @return a new StructureBuilder
   */
  default @NonNull T feed(@NonNull Iterator<Map<String, Object>> feeder) {
    return ScalaFeeds.apply(this, feeder);
  }

  /**
   * Attach a feed action.
   *
   * @param feeder a source of records
   * @param numberOfRecords the number of records to poll from the feeder at once
   * @return a new StructureBuilder
   */
  default @NonNull T feed(@NonNull Iterator<Map<String, Object>> feeder, int numberOfRecords) {
    return ScalaFeeds.apply(this, feeder, numberOfRecords);
  }

  /**
   * Attach a feed action.
   *
   * @param feeder a source of records
   * @param numberOfRecords the number of records to poll from the feeder at once
   * @return a new StructureBuilder
   */
  default @NonNull T feed(@NonNull Iterator<Map<String, Object>> feeder, String numberOfRecords) {
    return ScalaFeeds.apply(this, feeder, numberOfRecords);
  }

  /**
   * Attach a feed action.
   *
   * @param feeder a source of records
   * @param numberOfRecords the number of records to poll from the feeder at once
   * @return a new StructureBuilder
   */
  default @NonNull T feed(
      @NonNull Iterator<Map<String, Object>> feeder, Function<Session, Integer> numberOfRecords) {
    return ScalaFeeds.apply(this, feeder, numberOfRecords);
  }

  /**
   * Attach a feed action.
   *
   * @param feederBuilder a source of records
   * @return a new StructureBuilder
   */
  default @NonNull T feed(@NonNull FeederBuilder<?> feederBuilder) {
    return ScalaFeeds.apply(this, feederBuilder);
  }

  /**
   * Attach a feed action.
   *
   * @param feederBuilder a source of records
   * @param numberOfRecords the number of records to poll from the feeder at once
   * @return a new StructureBuilder
   */
  default @NonNull T feed(@NonNull FeederBuilder<?> feederBuilder, int numberOfRecords) {
    return ScalaFeeds.apply(this, feederBuilder, numberOfRecords);
  }

  /**
   * Attach a feed action.
   *
   * @param feederBuilder a source of records
   * @param numberOfRecords the number of records to poll from the feeder at once
   * @return a new StructureBuilder
   */
  default @NonNull T feed(@NonNull FeederBuilder<?> feederBuilder, String numberOfRecords) {
    return ScalaFeeds.apply(this, feederBuilder, numberOfRecords);
  }

  /**
   * Attach a feed action.
   *
   * @param feederBuilder a source of records
   * @param numberOfRecords the number of records to poll from the feeder at once
   * @return a new StructureBuilder
   */
  default @NonNull T feed(
      @NonNull FeederBuilder<?> feederBuilder, Function<Session, Integer> numberOfRecords) {
    return ScalaFeeds.apply(this, feederBuilder, numberOfRecords);
  }
}
