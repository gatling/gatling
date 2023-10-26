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

package io.gatling.shared.model.assertion

import boopickle.Default._

object AssertionPicklers {
  private object Picklers {
    private implicit val TimeMetricPickler: Pickler[TimeMetric] =
      compositePickler[TimeMetric]
        .addConcreteType[TimeMetric.ResponseTime.type]

    private implicit val CountMetricPickler: Pickler[CountMetric] =
      compositePickler[CountMetric]
        .addConcreteType[CountMetric.AllRequests.type]
        .addConcreteType[CountMetric.SuccessfulRequests.type]
        .addConcreteType[CountMetric.FailedRequests.type]

    private implicit val StatPickler: Pickler[Stat] =
      compositePickler[Stat]
        .addConcreteType[Stat.Min.type]
        .addConcreteType[Stat.Max.type]
        .addConcreteType[Stat.Mean.type]
        .addConcreteType[Stat.StandardDeviation.type]
        .addConcreteType[Stat.Percentile]

    private implicit val AssertionPathPickler: Pickler[AssertionPath] =
      compositePickler[AssertionPath]
        .addConcreteType[AssertionPath.Global.type]
        .addConcreteType[AssertionPath.ForAll.type]
        .addConcreteType[AssertionPath.Details]

    private implicit val TargetPickler: Pickler[Target] =
      compositePickler[Target]
        .addConcreteType[Target.Count]
        .addConcreteType[Target.Percent]
        .addConcreteType[Target.Time]
        .addConcreteType[Target.MeanRequestsPerSecond.type]

    private implicit val ConditionPickler: Pickler[Condition] =
      compositePickler[Condition]
        .addConcreteType[Condition.Lte]
        .addConcreteType[Condition.Gte]
        .addConcreteType[Condition.Lt]
        .addConcreteType[Condition.Gt]
        .addConcreteType[Condition.Is]
        .addConcreteType[Condition.Between]
        .addConcreteType[Condition.In]

    val AssertionPickler: Pickler[Assertion] = implicitly[Pickler[Assertion]]
  }

  implicit val AssertionPickler: Pickler[Assertion] = Picklers.AssertionPickler
}
