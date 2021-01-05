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

package io.gatling.core.structure

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PopulationBuilderSpec extends AnyFlatSpec with Matchers {

  private def newPopulationBuilder(name: String, children: List[PopulationBuilder]): PopulationBuilder =
    PopulationBuilder(
      scenarioBuilder = new ScenarioBuilder(name, Nil),
      injectionProfile = null,
      scenarioProtocols = Map.empty,
      scenarioThrottleSteps = Nil,
      pauseType = None,
      children = children,
      shard = false
    )

  "groupChildrenByParent" should "return empty flat populations" in {
    val population1 = newPopulationBuilder("scenario1", Nil)
    val population2 = newPopulationBuilder("scenario2", Nil)

    PopulationBuilder.groupChildrenByParent(List(population1, population2)) shouldBe Map.empty
  }

  it should "group children" in {
    val child1 = newPopulationBuilder("child1", Nil)
    val child2 = newPopulationBuilder("child2", Nil)
    val child3 = newPopulationBuilder("child3", Nil)

    val parent1 = newPopulationBuilder("parent1", List(child1, child2))
    val parent2 = newPopulationBuilder("parent2", List(child3))
    val parent3 = newPopulationBuilder("parent3", Nil)

    PopulationBuilder.groupChildrenByParent(List(parent1, parent2, parent3)) shouldBe Map(
      parent1.scenarioBuilder.name -> List(child1, child2),
      parent2.scenarioBuilder.name -> List(child3)
    )
  }
}
