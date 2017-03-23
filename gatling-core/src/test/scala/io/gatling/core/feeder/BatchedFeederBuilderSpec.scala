/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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
package io.gatling.core.feeder

import io.gatling.BaseSpec
import io.gatling.core.feeder._
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.feeder._

class BatchedFeederBuilderSpec extends BaseSpec {
  "BatchedFeederBuilder" should "support Random strategy" in {
    val records = (0 to 20).map { i =>
      Map("id" -> i)
    }.toIterator

    val ctx = mock[ScenarioContext]
    val builder = BatchedFeederBuilder(records, 2L, 3L, Random)
    val feeder = builder.build(ctx)

    feeder.hasNext shouldBe true
    feeder.next()("id") should (equal(0) or equal(1))

    feeder.hasNext shouldBe true
    feeder.next()("id") should (equal(0) or equal(1))

    feeder.hasNext shouldBe true
    feeder.next()("id") should (equal(0) or equal(1))

    feeder.hasNext shouldBe true
    feeder.next()("id") should (equal(2) or equal(3))
  }

  "BatchedFeederBuilder" should "support Queue strategy" in {
    val records = (0 to 20).map { i =>
      Map("id" -> i)
    }.toIterator

    val ctx = mock[ScenarioContext]
    val builder = BatchedFeederBuilder(records, 2L, 3L, Queue)
    val feeder = builder.build(ctx)

    feeder.hasNext shouldBe true
    feeder.next()("id") shouldBe 0

    feeder.hasNext shouldBe true
    feeder.next()("id") shouldBe 1

    feeder.hasNext shouldBe false

    feeder.hasNext shouldBe true
    feeder.next()("id") shouldBe 2

    feeder.hasNext shouldBe true
    feeder.next()("id") shouldBe 3

    feeder.hasNext shouldBe false
  }

  "BatchedFeederBuilder" should "support Shuffle strategy" in {
    val records = (0 to 20).map { i =>
      Map("id" -> i)
    }.toIterator

    val ctx = mock[ScenarioContext]
    val builder = BatchedFeederBuilder(records, 2L, 3L, Shuffle)
    val feeder = builder.build(ctx)

    feeder.hasNext shouldBe true
    feeder.next()("id") should (equal(0) or equal(1))

    feeder.hasNext shouldBe true
    feeder.next()("id") should (equal(0) or equal(1))

    feeder.hasNext shouldBe false

    feeder.hasNext shouldBe true
    feeder.next()("id") should (equal(2) or equal(3))

    feeder.hasNext shouldBe true
    feeder.next()("id") should (equal(2) or equal(3))

    feeder.hasNext shouldBe false
  }

  "BatchedFeederBuilder" should "support Circular strategy" in {
    val records = (0 to 20).map { i =>
      Map("id" -> i)
    }.toIterator

    val ctx = mock[ScenarioContext]
    val builder = BatchedFeederBuilder(records, 2L, 3L, Circular)
    val feeder = builder.build(ctx)

    feeder.hasNext shouldBe true
    feeder.next()("id") shouldBe 0

    feeder.hasNext shouldBe true
    feeder.next()("id") shouldBe 1

    feeder.hasNext shouldBe true
    feeder.next()("id") shouldBe 0

    feeder.hasNext shouldBe true
    feeder.next()("id") shouldBe 2

    feeder.hasNext shouldBe true
    feeder.next()("id") shouldBe 3

    feeder.hasNext shouldBe true
    feeder.next()("id") shouldBe 2
  }
}
