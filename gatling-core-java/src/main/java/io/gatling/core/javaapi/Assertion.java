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

package io.gatling.core.javaapi;

import io.gatling.commons.stats.assertion.*;
import io.gatling.core.config.GatlingConfiguration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static io.gatling.core.javaapi.internal.ScalaHelpers.toScalaSeq;

public final class Assertion {

  private final io.gatling.commons.stats.assertion.Assertion wrapped;

  public Assertion(io.gatling.commons.stats.assertion.Assertion wrapped) {
    this.wrapped = wrapped;
  }

  public io.gatling.commons.stats.assertion.Assertion asScala() {
    return wrapped;
  }

  public static final class WithPath {
    private final AssertionPath path;
    private final GatlingConfiguration configuration;

    public WithPath(AssertionPath path, GatlingConfiguration configuration) {
      this.path = path;
      this.configuration = configuration;
    }

    public WithPathAndTimeMetric responseTime() {
      return new WithPathAndTimeMetric(path, ResponseTime$.MODULE$, configuration);
    }

    public WithPathAndCountMetric allRequests() {
      return new WithPathAndCountMetric(path, AllRequests$.MODULE$);
    }

    public WithPathAndCountMetric failedRequests() {
      return new WithPathAndCountMetric(path, FailedRequests$.MODULE$);
    }

    public WithPathAndCountMetric successfulRequests() {
      return new WithPathAndCountMetric(path, SuccessfulRequests$.MODULE$);
    }

    public WithPathAndTarget<Double> requestsPerSec() {
      return new WithPathAndTarget<>(path, MeanRequestsPerSecondTarget$.MODULE$);
    }
  }

  public static final class WithPathAndTimeMetric {
    private final AssertionPath path;
    private final TimeMetric metric;
    private final GatlingConfiguration configuration;

    public WithPathAndTimeMetric(AssertionPath path, TimeMetric metric, GatlingConfiguration configuration) {
      this.path = path;
      this.metric = metric;
      this.configuration = configuration;
    }

    private WithPathAndTarget<Integer> next(TimeSelection selection) {
      return new WithPathAndTarget<>(path, new TimeTarget(metric, selection));
    }

    public WithPathAndTarget<Integer> min() {
      return next(Min$.MODULE$);
    }

    public WithPathAndTarget<Integer> max() {
      return next(Max$.MODULE$);
    }

    public WithPathAndTarget<Integer> mean() {
      return next(Mean$.MODULE$);
    }

    public WithPathAndTarget<Integer> stdDev() {
      return next(StandardDeviation$.MODULE$);
    }

    public WithPathAndTarget<Integer> percentile1() {
      return percentile(configuration.charting().indicators().percentile1());
    }

    public WithPathAndTarget<Integer> percentile2() {
      return percentile(configuration.charting().indicators().percentile2());
    }

    public WithPathAndTarget<Integer> percentile3() {
      return percentile(configuration.charting().indicators().percentile3());
    }

    public WithPathAndTarget<Integer> percentile4() {
      return percentile(configuration.charting().indicators().percentile4());
    }

    public WithPathAndTarget<Integer> percentile(double value) {
      return next(new Percentiles(value));
    }
  }

  public static final class WithPathAndCountMetric {
    private final AssertionPath path;
    private final CountMetric metric;

    public WithPathAndCountMetric(AssertionPath path, CountMetric metric) {
      this.path = path;
      this.metric = metric;
    }

    public WithPathAndTarget<Long> count() {
      return new WithPathAndTarget<>(path, new CountTarget(metric));
    }

    public WithPathAndTarget<Double> percent() {
      return new WithPathAndTarget<>(path, new PercentTarget(metric));
    }
  }

  public static final class WithPathAndTarget<T extends Number> {
    private final AssertionPath path;
    private final Target target;

    public WithPathAndTarget(AssertionPath path, Target target) {
      this.path = path;
      this.target = target;
    }

    public Assertion next(Condition condition) {
      return new Assertion(new io.gatling.commons.stats.assertion.Assertion(path, target, condition));
    }

    public Assertion lt(T threshold) {
      return next(new Lt(threshold.doubleValue()));
    }

    public Assertion lte(T threshold) {
      return next(new Lte(threshold.doubleValue()));
    }

    public Assertion gt(T threshold) {
      return next(new Gt(threshold.doubleValue()));
    }

    public Assertion gte(T threshold) {
      return next(new Gte(threshold.doubleValue()));
    }

    public Assertion between(T min, T max) {
      return between(min, max, true);
    }

    public Assertion between(T min, T max, boolean inclusive) {
      return next(new Between(min.doubleValue(), max.doubleValue(), inclusive));
    }

    public Assertion around(T mean, T plusOrMinus) {
      return around(mean, plusOrMinus, true);
    }

    public Assertion around(T mean, T plusOrMinus, boolean inclusive) {
      return next(new Between(mean.doubleValue() - plusOrMinus.doubleValue(), mean.doubleValue() + plusOrMinus.doubleValue(), inclusive));
    }

    public Assertion deviatesAround(T target, double percentDeviationThreshold, boolean inclusive) {
      double margin = Math.floor(target.doubleValue() * percentDeviationThreshold);
      return next(new Between(target.doubleValue() - margin, target.doubleValue() + margin, inclusive));
    }

    public Assertion is(T value) {
      return next(new Is(value.doubleValue()));
    }

    public Assertion in(T... values) {
      return in(Arrays.stream(values).collect(Collectors.toSet()));
    }

    public Assertion in(Set<T> values) {
      return next(new In(toScalaSeq(values.stream().map(value -> (Object) value.doubleValue()).collect(Collectors.toList())).toList()));
    }
  }
}
