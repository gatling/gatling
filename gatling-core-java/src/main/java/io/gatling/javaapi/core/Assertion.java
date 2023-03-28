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

package io.gatling.javaapi.core;

import static io.gatling.javaapi.core.internal.Converters.toScalaSeq;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.gatling.commons.stats.assertion.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Java wrapper of a Scala Assertion
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class Assertion {

  private final io.gatling.commons.stats.assertion.Assertion wrapped;

  Assertion(io.gatling.commons.stats.assertion.Assertion wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * For internal use only
   *
   * @return the wrapped Scala instance
   */
  public io.gatling.commons.stats.assertion.Assertion asScala() {
    return wrapped;
  }

  /**
   * Step 2 of the Assertion DSL (path defined) Immutable, so all methods return a new occurrence
   * and leave the original unmodified.
   */
  public static final class WithPath {
    private final AssertionPath path;

    WithPath(AssertionPath path) {
      this.path = path;
    }

    /**
     * Specify the Assertion targets the response time metric
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTimeMetric responseTime() {
      return new WithPathAndTimeMetric(path, ResponseTime$.MODULE$);
    }

    /**
     * Specify the Assertion targets the all requests count metric
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndCountMetric allRequests() {
      return new WithPathAndCountMetric(path, AllRequests$.MODULE$);
    }

    /**
     * Specify the Assertion targets the failed requests count metric
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndCountMetric failedRequests() {
      return new WithPathAndCountMetric(path, FailedRequests$.MODULE$);
    }

    /**
     * Specify the Assertion targets the successful requests count metric
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndCountMetric successfulRequests() {
      return new WithPathAndCountMetric(path, SuccessfulRequests$.MODULE$);
    }

    /**
     * Specify the Assertion targets the requests/s metric
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTarget<Double> requestsPerSec() {
      return new WithPathAndTarget<>(path, MeanRequestsPerSecondTarget$.MODULE$);
    }
  }

  /**
   * Step 3 of the Assertion DSL (path and time metric defined) Immutable, so all methods return a
   * new occurrence and leave the original unmodified.
   */
  public static final class WithPathAndTimeMetric {
    private final AssertionPath path;
    private final TimeMetric metric;

    private WithPathAndTimeMetric(AssertionPath path, TimeMetric metric) {
      this.path = path;
      this.metric = metric;
    }

    private WithPathAndTarget<Integer> next(TimeSelection selection) {
      return new WithPathAndTarget<>(path, new TimeTarget(metric, selection));
    }

    /**
     * Specify the Assertion targets the min value metric
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTarget<Integer> min() {
      return next(Min$.MODULE$);
    }

    /**
     * Specify the Assertion targets the max value metric
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTarget<Integer> max() {
      return next(Max$.MODULE$);
    }

    /**
     * Specify the Assertion targets the mean value metric
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTarget<Integer> mean() {
      return next(Mean$.MODULE$);
    }

    /**
     * Specify the Assertion targets the standard deviation metric
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTarget<Integer> stdDev() {
      return next(StandardDeviation$.MODULE$);
    }

    /**
     * Specify the Assertion targets the percentile1 metric, as defined in gatling.conf
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTarget<Integer> percentile1() {
      return percentile(
          io.gatling.core.Predef.configuration().charting().indicators().percentile1());
    }

    /**
     * Specify the Assertion targets the percentile2 metric, as defined in gatling.conf
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTarget<Integer> percentile2() {
      return percentile(
          io.gatling.core.Predef.configuration().charting().indicators().percentile2());
    }

    /**
     * Specify the Assertion targets the percentile3 metric, as defined in gatling.conf
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTarget<Integer> percentile3() {
      return percentile(
          io.gatling.core.Predef.configuration().charting().indicators().percentile3());
    }

    /**
     * Specify the Assertion targets the percentile4 metric, as defined in gatling.conf
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTarget<Integer> percentile4() {
      return percentile(
          io.gatling.core.Predef.configuration().charting().indicators().percentile4());
    }

    /**
     * Specify the Assertion targets the given percentile metric
     *
     * @param value the value of targeted percentile, between 0 and 100)
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTarget<Integer> percentile(double value) {
      return next(new Percentiles(value));
    }
  }

  /**
   * Step 3 of the Assertion DSL (path and count metric defined) Immutable, so all methods return a
   * new occurrence and leave the original unmodified.
   */
  public static final class WithPathAndCountMetric {
    private final AssertionPath path;
    private final CountMetric metric;

    private WithPathAndCountMetric(AssertionPath path, CountMetric metric) {
      this.path = path;
      this.metric = metric;
    }

    /**
     * Specify the Assertion targets the count metric
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTarget<Long> count() {
      return new WithPathAndTarget<>(path, new CountTarget(metric));
    }

    /**
     * Specify the Assertion targets the percentage of total executions metric
     *
     * @return the next Assertion DSL step
     */
    @NonNull
    public WithPathAndTarget<Double> percent() {
      return new WithPathAndTarget<>(path, new PercentTarget(metric));
    }
  }

  /**
   * Step 4 of the Assertion DSL (path and target defined) Immutable, so all methods return a new
   * occurrence and leave the original unmodified.
   */
  public static final class WithPathAndTarget<T extends Number> {
    private final AssertionPath path;
    private final Target target;

    private WithPathAndTarget(AssertionPath path, Target target) {
      this.path = path;
      this.target = target;
    }

    private Assertion next(Condition condition) {
      return new Assertion(
          new io.gatling.commons.stats.assertion.Assertion(path, target, condition));
    }

    /**
     * Specify the metric must be strictly less than the expected value
     *
     * @param value the value
     * @return a complete Assertion
     */
    @NonNull
    public Assertion lt(T value) {
      return next(new Lt(value.doubleValue()));
    }

    /**
     * Specify the metric must be less than or equal to the expected value
     *
     * @param value the value
     * @return a complete Assertion
     */
    @NonNull
    public Assertion lte(T value) {
      return next(new Lte(value.doubleValue()));
    }

    /**
     * Specify the metric must be strictly greater than the expected value
     *
     * @param value the value
     * @return a complete Assertion
     */
    @NonNull
    public Assertion gt(T value) {
      return next(new Gt(value.doubleValue()));
    }

    /**
     * Specify the metric must be greater than or equal to the expected value
     *
     * @param value the value
     * @return a complete Assertion
     */
    @NonNull
    public Assertion gte(T value) {
      return next(new Gte(value.doubleValue()));
    }

    /**
     * Specify the metric must be included in the given range, bounds included
     *
     * @param min the min, included
     * @param max the max, included
     * @return a complete Assertion
     */
    @NonNull
    public Assertion between(T min, T max) {
      return between(min, max, true);
    }

    /**
     * Specify the metric must be included in the given range
     *
     * @param min the min, included
     * @param max the max, included
     * @param inclusive if bounds must be included in the range
     * @return a complete Assertion
     */
    @NonNull
    public Assertion between(T min, T max, boolean inclusive) {
      return next(new Between(min.doubleValue(), max.doubleValue(), inclusive));
    }

    /**
     * Specify the metric must be included in a range defined around a mean value with a half range
     * expressed as an absolute value, bounds included
     *
     * @param mean the mean of the range
     * @param plusOrMinus the range half width
     * @return a complete Assertion
     */
    @NonNull
    public Assertion around(T mean, T plusOrMinus) {
      return around(mean, plusOrMinus, true);
    }

    /**
     * Specify the metric must be included in a range defined around a mean value with a half range
     * expressed as an absolute value
     *
     * @param mean the mean of the range
     * @param plusOrMinus the range half width
     * @param inclusive if bounds must be included in the range
     * @return a complete Assertion
     */
    @NonNull
    public Assertion around(T mean, T plusOrMinus, boolean inclusive) {
      return next(
          new Between(
              mean.doubleValue() - plusOrMinus.doubleValue(),
              mean.doubleValue() + plusOrMinus.doubleValue(),
              inclusive));
    }

    /**
     * Specify the metric must be included in a range defined around a mean value with a half range
     * expressed as an percentage of the mean value, bounds included
     *
     * @param mean the mean of the range
     * @param percentDeviation the range half width expressed as a percent of the mean
     * @return a complete Assertion
     */
    public Assertion deviatesAround(T mean, double percentDeviation) {
      return deviatesAround(mean, percentDeviation, true);
    }

    /**
     * Specify the metric must be included in a range defined around a mean value with a half range
     * expressed as an percentage of the mean value
     *
     * @param mean the mean of the range
     * @param percentDeviation the range half width expressed as a percent of the mean
     * @param inclusive if bounds must be included in the range
     * @return a complete Assertion
     */
    public Assertion deviatesAround(T mean, double percentDeviation, boolean inclusive) {
      double margin = Math.floor(mean.doubleValue() * percentDeviation);
      return next(new Between(mean.doubleValue() - margin, mean.doubleValue() + margin, inclusive));
    }

    /**
     * Specify the metric must be equal to an expected value
     *
     * @param value the expected value
     * @return a complete Assertion
     */
    @NonNull
    public Assertion is(T value) {
      return next(new Is(value.doubleValue()));
    }

    /**
     * Alias for {@link WithPathAndTarget#is(Number)} as `is` is a reserved keyword in Kotlin
     *
     * @param value the expected value
     * @return a complete Assertion
     */
    @NonNull
    public Assertion shouldBe(T value) {
      return is(value);
    }

    /**
     * Specify the metric must be included in a set of values
     *
     * @param values the expected values
     * @return a complete Assertion
     */
    @NonNull
    public Assertion in(T... values) {
      return in(Arrays.stream(values).collect(Collectors.toSet()));
    }

    /**
     * Alias for `in` that's a reserved keyword in Kotlin
     *
     * @param values the expected values
     * @return a complete Assertion
     */
    @NonNull
    public Assertion within(T... values) {
      return in(values);
    }

    /**
     * Specify the metric must be included in a set of values
     *
     * @param values the expected values
     * @return a complete Assertion
     */
    @NonNull
    public Assertion in(Set<T> values) {
      return next(
          new In(
              toScalaSeq(
                      values.stream()
                          .map(value -> (Object) value.doubleValue())
                          .collect(Collectors.toList()))
                  .toList()));
    }

    /**
     * Alias for `in` that's a reserved keyword in Kotlin
     *
     * @param values the expected values
     * @return a complete Assertion
     */
    @NonNull
    public Assertion within(Set<T> values) {
      return in(values);
    }
  }
}
