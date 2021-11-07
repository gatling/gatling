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

package io.gatling.javaapi.core;

import io.gatling.core.feeder.BatchableFeederBuilder;
import io.gatling.core.feeder.SeparatedValuesParser;
import io.gatling.javaapi.core.internal.Converters;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * Builder of feeders, ie sources of data that are shared amongst all virtual users. Typically
 * backed by an underlying resource, such as a CSV file.
 *
 * <p>Immutable so each method doesn't mutate the current instance but returns a new one.
 *
 * @param <T> the type of values the feeder will provide
 */
public interface FeederBuilder<T> {

  /**
   * Set a queue strategy. Records will be provided in the same order as defined in the underlying
   * source. A given record will only be provided once. The run will be immediately stopped if the
   * feeder runs out of records.
   *
   * @return a new FeederBuilder
   */
  @Nonnull
  FeederBuilder<T> queue();

  /**
   * Set a random strategy. Records will be provided in a random order, unrelated to the order in
   * the underlying source. A given record can be provided multiple times. Such feeder will never
   * run out of records.
   *
   * @return a new FeederBuilder
   */
  @Nonnull
  FeederBuilder<T> random();

  /**
   * Set a shuffle strategy. Records will be provided in a random order, unrelated to the order in
   * the underlying source. A given record will only be provided once. The run will be immediately
   * stopped if the feeder runs out of records.
   *
   * @return a new FeederBuilder
   */
  @Nonnull
  FeederBuilder<T> shuffle();

  /**
   * Set a circular strategy. Records will be provided in the same order as defined in the
   * underlying source. Once the last record of the underlying source is reached, the feeder will go
   * back to the first record. A given record can be provided multiple times. Such feeder will never
   * run out of records.
   *
   * @return a new FeederBuilder
   */
  @Nonnull
  FeederBuilder<T> circular();

  /**
   * Provide a function to transform records as defined in the underlying source
   *
   * @param f the transformation function
   * @return a new FeederBuilder
   */
  @Nonnull
  FeederBuilder<Object> transform(@Nonnull BiFunction<String, T, Object> f);

  /**
   * Read all the records of the underlying source.
   *
   * @return the whole data
   */
  @Nonnull
  List<Map<String, Object>> readRecords();

  /**
   * Distribute data evenly amongst all the injectors of a Gatling Enterprise cluster. Only
   * effective when the test is running with Gatling Enterprise, noop otherwise.
   *
   * @return a new FeederBuilder
   */
  @Nonnull
  FeederBuilder<T> shard();

  /**
   * For internal use only
   *
   * @return the wrapped Scala instance
   */
  scala.Function0<scala.collection.Iterator<scala.collection.immutable.Map<String, Object>>>
      asScala();

  /**
   * A {@link FeederBuilder} that is backed by a file.
   *
   * @param <T> the type of values the feeder will provide
   */
  interface FileBased<T> extends FeederBuilder<T> {
    @Override
    @Nonnull
    FileBased<T> queue();

    @Override
    @Nonnull
    FileBased<T> random();

    @Override
    @Nonnull
    FileBased<T> shuffle();

    @Override
    @Nonnull
    FileBased<T> circular();

    @Override
    @Nonnull
    FileBased<T> shard();

    /**
     * Advice to unzip the underlying source because it's a zip or tar file
     *
     * @return a new FileBased
     */
    @Nonnull
    FileBased<T> unzip();
  }

  /**
   * A {@link FileBased} whose records can be fetched in batches. If not forced, loading strategy
   * will be picked bepending on the size of the underlying data source. "eager" will be preferred
   * for small data and "batch" otherwise.
   *
   * @param <T> the type of values the feeder will provide
   */
  interface Batchable<T> extends FileBased<T> {
    @Override
    @Nonnull
    Batchable<T> queue();

    @Override
    @Nonnull
    Batchable<T> random();

    @Override
    @Nonnull
    Batchable<T> shuffle();

    @Override
    @Nonnull
    Batchable<T> circular();

    @Override
    @Nonnull
    Batchable<T> shard();

    @Override
    @Nonnull
    Batchable<T> unzip();

    /**
     * Force loading the whole data in memory from the underlying source at once. Faster runtime but
     * slower boot time and higher heap usage.
     *
     * @return a new Batchable
     */
    @Nonnull
    Batchable<T> eager();

    /**
     * Force loading small chunks of data from the underlying source one by one. Slower runtime but
     * faster boot time and lower memory consumption.
     *
     * @return a new Batchable
     */
    @Nonnull
    Batchable<T> batch();

    /**
     * Force loading small chunks of data from the underlying source one by one Slower runtime but
     * faster boot time and lower memory consumption.
     *
     * @param lines the number of buffered lines
     * @return a new Batchable
     */
    @Nonnull
    Batchable<T> batch(int lines);
  }

  final class Impl<T> implements Batchable<T> {
    private final io.gatling.core.feeder.BatchableFeederBuilder<T> wrapped;

    @Nonnull
    static Batchable<String> csv(@Nonnull String filePath) {
      return csv(filePath, SeparatedValuesParser.DefaultQuoteChar());
    }

    @Nonnull
    static Batchable<String> csv(@Nonnull String filePath, char quoteChar) {
      return new Impl<>(
          io.gatling.core.Predef.csv(filePath, quoteChar, io.gatling.core.Predef.configuration()));
    }

    @Nonnull
    static Batchable<String> ssv(@Nonnull String filePath) {
      return ssv(filePath, SeparatedValuesParser.DefaultQuoteChar());
    }

    @Nonnull
    static Batchable<String> ssv(@Nonnull String filePath, char quoteChar) {
      return new Impl<>(
          io.gatling.core.Predef.ssv(filePath, quoteChar, io.gatling.core.Predef.configuration()));
    }

    @Nonnull
    static Batchable<String> tsv(@Nonnull String filePath) {
      return tsv(filePath, SeparatedValuesParser.DefaultQuoteChar());
    }

    @Nonnull
    static Batchable<String> tsv(@Nonnull String filePath, char quoteChar) {
      return new Impl<>(
          io.gatling.core.Predef.tsv(filePath, quoteChar, io.gatling.core.Predef.configuration()));
    }

    @Nonnull
    static Batchable<String> separatedValues(@Nonnull String filePath, char separator) {
      return separatedValues(filePath, separator, SeparatedValuesParser.DefaultQuoteChar());
    }

    @Nonnull
    static Batchable<String> separatedValues(
        @Nonnull String filePath, char separator, char quoteChar) {
      return new Impl<>(
          io.gatling.core.Predef.separatedValues(
              filePath, separator, quoteChar, io.gatling.core.Predef.configuration()));
    }

    @Nonnull
    static FileBased<Object> jsonFile(@Nonnull String filePath) {
      return new Impl<>(
          io.gatling.core.Predef.jsonFile(
              filePath,
              io.gatling.core.Predef.defaultJsonParsers(),
              io.gatling.core.Predef.configuration()));
    }

    @Nonnull
    static FeederBuilder<Object> jsonUrl(@Nonnull String url) {
      return new Impl<>(
          io.gatling.core.Predef.jsonUrl(
              url,
              io.gatling.core.Predef.defaultJsonParsers(),
              io.gatling.core.Predef.configuration()));
    }

    public Impl(@Nonnull io.gatling.core.feeder.FeederBuilderBase<T> wrapped) {
      this.wrapped = (io.gatling.core.feeder.BatchableFeederBuilder<T>) wrapped;
    }

    private Impl<T> make(
        Function<
                io.gatling.core.feeder.BatchableFeederBuilder<T>,
                io.gatling.core.feeder.FeederBuilderBase<T>>
            f) {
      return new Impl<>(f.apply(wrapped));
    }

    @Override
    @Nonnull
    public Batchable<T> queue() {
      return make(BatchableFeederBuilder::queue);
    }

    @Override
    @Nonnull
    public Batchable<T> random() {
      return make(BatchableFeederBuilder::random);
    }

    @Override
    @Nonnull
    public Batchable<T> shuffle() {
      return make(BatchableFeederBuilder::shuffle);
    }

    @Override
    @Nonnull
    public Batchable<T> circular() {
      return make(BatchableFeederBuilder::circular);
    }

    @Override
    @Nonnull
    public FeederBuilder<Object> transform(@Nonnull BiFunction<String, T, Object> f) {
      return new Impl<>(
          wrapped.transform(
              new scala.PartialFunction<scala.Tuple2<String, T>, Object>() {

                @Override
                public boolean isDefinedAt(scala.Tuple2<String, T> x) {
                  return true;
                }

                @Override
                public Object apply(scala.Tuple2<String, T> v1) {
                  return f.apply(v1._1, v1._2);
                }
              }));
    }

    @Override
    @Nonnull
    public List<Map<String, Object>> readRecords() {
      return Converters.toJavaList(wrapped.readRecords()).stream()
          .map(Converters::toJavaMap)
          .collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public Batchable<T> shard() {
      return make(BatchableFeederBuilder::shard);
    }

    @Override
    @Nonnull
    public Batchable<T> unzip() {
      return make(BatchableFeederBuilder::unzip);
    }

    @Override
    @Nonnull
    public Batchable<T> eager() {
      return make(BatchableFeederBuilder::eager);
    }

    @Override
    @Nonnull
    public Batchable<T> batch() {
      return make(BatchableFeederBuilder::batch);
    }

    @Override
    @Nonnull
    public Batchable<T> batch(int lines) {
      return make(wrapped -> wrapped.batch(lines));
    }

    @Override
    public scala.Function0<
            scala.collection.Iterator<scala.collection.immutable.Map<String, Object>>>
        asScala() {
      return wrapped;
    }
  }
}
