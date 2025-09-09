/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.core.controller.inject.closed.ClosedInjectionSupport;
import io.gatling.core.controller.inject.open.OpenInjectionSupport;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;

/**
 * Java wrapper of a Scala ScenarioBuilder.
 *
 * <p>Immutable, so all methods return a new occurrence and leave the original unmodified.
 */
public final class ScenarioBuilder
    extends StructureBuilder<ScenarioBuilder, io.gatling.core.structure.ScenarioBuilder> {

  ScenarioBuilder(@NonNull String name) {
    this(io.gatling.core.Predef.scenario(name));
  }

  ScenarioBuilder(io.gatling.core.structure.@NonNull ScenarioBuilder wrapped) {
    super(wrapped);
  }

  public @NonNull PopulationBuilder injectOpen(@NonNull OpenInjectionStep... steps) {
    return injectOpen(Arrays.asList(steps));
  }

  public @NonNull PopulationBuilder injectOpen(@NonNull List<OpenInjectionStep> steps) {
    List<io.gatling.core.controller.inject.open.OpenInjectionStep> scalaSteps =
        steps.stream().map(OpenInjectionStep::asScala).collect(Collectors.toList());
    return new PopulationBuilder(
        wrapped.inject(
            OpenInjectionSupport.OpenInjectionProfileFactory().profile(toScalaSeq(scalaSteps))));
  }

  public @NonNull PopulationBuilder injectClosed(@NonNull ClosedInjectionStep... steps) {
    return injectClosed(Arrays.asList(steps));
  }

  public @NonNull PopulationBuilder injectClosed(@NonNull List<ClosedInjectionStep> steps) {
    List<io.gatling.core.controller.inject.closed.ClosedInjectionStep> scalaSteps =
        steps.stream().map(ClosedInjectionStep::asScala).collect(Collectors.toList());
    return new PopulationBuilder(
        wrapped.inject(
            ClosedInjectionSupport.ClosedInjectionProfileFactory()
                .profile(toScalaSeq(scalaSteps))));
  }

  @Override
  public ScenarioBuilder make(
      Function<io.gatling.core.structure.ScenarioBuilder, io.gatling.core.structure.ScenarioBuilder>
          f) {
    return new ScenarioBuilder(f.apply(wrapped));
  }
}
