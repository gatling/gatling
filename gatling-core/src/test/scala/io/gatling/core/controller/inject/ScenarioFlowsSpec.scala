/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.core.controller.inject

import io.gatling.BaseSpec

class ScenarioFlowsSpec extends BaseSpec {
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  private def node(key: String, childrenSequences: List[List[ScenarioFlows.Node[String, String]]] = Nil): ScenarioFlows.Node[String, String] =
    ScenarioFlows.Node(key, key, childrenSequences)

  private val rootNodes = List(
    node(
      "scn1",
      List(
        List(node("scn1.1", List(List(node("scn1.1.1"), node("scn1.1.2")), List(node("scn1.1.3"), node("scn1.1.4"))))),
        List(node("scn1.2"))
      )
    ),
    node("scn2")
  )

  "fromNodes" should "compute complex flows" in {
    ScenarioFlows.fromNodes(rootNodes).locks.sortBy(_.value) shouldBe List(
      ScenarioFlows.Flow("scn1", Set.empty),
      ScenarioFlows.Flow("scn1.1", Set("scn1")),
      ScenarioFlows.Flow("scn1.1.1", Set("scn1.1")),
      ScenarioFlows.Flow("scn1.1.2", Set("scn1.1")),
      ScenarioFlows.Flow("scn1.1.3", Set("scn1.1.1", "scn1.1.2")),
      ScenarioFlows.Flow("scn1.1.4", Set("scn1.1.1", "scn1.1.2")),
      ScenarioFlows.Flow("scn1.2", Set("scn1.1.3", "scn1.1.4")),
      ScenarioFlows.Flow("scn2", Set.empty)
    )
  }

  "remove and extractReady" should "trigger children flows" in {
    val (roots, initialState) = ScenarioFlows.fromNodes(rootNodes).extractReady
    roots.toSet shouldBe Set("scn1", "scn2")
    initialState.isEmpty shouldBe false

    val (readyAfterScn1, stateAfterScn1) = initialState.remove("scn1").extractReady
    readyAfterScn1.toSet shouldBe Set("scn1.1")
    stateAfterScn1.isEmpty shouldBe false

    val (readyAfterScn2, stateAfterScn2) = stateAfterScn1.remove("scn2").extractReady
    readyAfterScn2 shouldBe empty
    stateAfterScn2.isEmpty shouldBe false

    val (readyAfterScn1_1, stateAfterScn1_1) = stateAfterScn2.remove("scn1.1").extractReady
    readyAfterScn1_1.toSet shouldBe Set("scn1.1.1", "scn1.1.2")
    stateAfterScn1_1.isEmpty shouldBe false

    val (readyAfterScn1_1_1, stateAfterScn1_1_1) = stateAfterScn1_1.remove("scn1.1.1").extractReady
    readyAfterScn1_1_1 shouldBe empty
    stateAfterScn1_1_1.isEmpty shouldBe false

    val (readyAfterScn1_1_2, stateAfterScn1_1_2) = stateAfterScn1_1_1.remove("scn1.1.2").extractReady
    readyAfterScn1_1_2.toSet shouldBe Set("scn1.1.3", "scn1.1.4")
    stateAfterScn1_1_2.isEmpty shouldBe false

    val (readyAfterScn1_1_3, stateAfterScn1_1_3) = stateAfterScn1_1_2.remove("scn1.1.3").extractReady
    readyAfterScn1_1_3 shouldBe empty
    stateAfterScn1_1_3.isEmpty shouldBe false

    val (readyAfterScn1_1_4, stateAfterScn1_1_4) = stateAfterScn1_1_3.remove("scn1.1.4").extractReady
    readyAfterScn1_1_4.toSet shouldBe Set("scn1.2")
    stateAfterScn1_1_4.isEmpty shouldBe true

    val (readyAfterScn1_2, stateAfterScn1_2) = stateAfterScn1_1_4.remove("scn1.2").extractReady
    readyAfterScn1_2 shouldBe empty
    stateAfterScn1_2.isEmpty shouldBe true
  }
}
