/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.commons.stats.assertion

import boopickle.Default._

object AssertionPicklers {

  private implicit val AssertionPathPickler: Pickler[AssertionPath] =
    compositePickler[AssertionPath]
      .addConcreteType[Global.type]
      .addConcreteType[ForAll.type]
      .addConcreteType[Details]

  private implicit val TargetPickler: Pickler[Target] =
    compositePickler[Target]
      .addConcreteType[CountTarget]
      .addConcreteType[PercentTarget]
      .addConcreteType[TimeTarget]
      .addConcreteType[MeanRequestsPerSecondTarget.type]

  private implicit val ConditionPickler: Pickler[Condition] =
    compositePickler[Condition]
      .addConcreteType[Lte]
      .addConcreteType[Gte]
      .addConcreteType[Lt]
      .addConcreteType[Gt]
      .addConcreteType[Is]
      .addConcreteType[Between]
      .addConcreteType[In]

  implicit val AssertionPickler: Pickler[Assertion] = implicitly[Pickler[Assertion]]
}
