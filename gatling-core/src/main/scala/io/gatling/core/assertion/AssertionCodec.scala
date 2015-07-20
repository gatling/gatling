/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.assertion

import boopickle._
import boopickle.Default._

trait AssertionCodec {

  implicit val defaultPathPicklerPair =
    CompositePickler[Path]
      .addConcreteType[Global.type]
      .addConcreteType[ForAll.type]
      .addConcreteType[Details]

  implicit val defaultConditionPicklerPair =
    CompositePickler[Condition]
      .addConcreteType[LessThan]
      .addConcreteType[GreaterThan]
      .addConcreteType[Is]
      .addConcreteType[Between]
      .addConcreteType[In]

  implicit val defaultTimeSelectionPicklerPair =
    CompositePickler[TimeSelection]
      .addConcreteType[Min.type]
      .addConcreteType[Max.type]
      .addConcreteType[Mean.type]
      .addConcreteType[StandardDeviation.type]
      .addConcreteType[Percentiles]

  implicit val defaultCountSelectionPicklerPair =
    CompositePickler[CountSelection]
      .addConcreteType[Count.type]
      .addConcreteType[Percent.type]
      .addConcreteType[PerMillion.type]

  implicit val defaultTimeMetricPicklerPair =
    CompositePickler[TimeMetric]
      .addConcreteType[ResponseTime.type]

  implicit val defaultCountMetricPicklerPair =
    CompositePickler[CountMetric]
      .addConcreteType[AllRequests.type]
      .addConcreteType[FailedRequests.type]
      .addConcreteType[SuccessfulRequests.type]

  implicit val defaultTargetPicklerPair =
    CompositePickler[Target]
      .addConcreteType[CountTarget]
      .addConcreteType[TimeTarget]
      .addConcreteType[MeanRequestsPerSecondTarget.type]
}
