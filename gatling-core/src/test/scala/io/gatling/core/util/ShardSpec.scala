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

package io.gatling.core.util

import io.gatling.BaseSpec

import org.scalacheck.Gen

class ShardSpec extends BaseSpec {

  "Shard.shards" should "return the proper count" in {
    forAll(Gen.choose(1, Int.MaxValue)) { total =>
      whenever(total >= 1) {
        Shard.shards(total, 1000).sum shouldBe total
      }
    }
  }

  it should "return the proper count 2" in {
    val shards = Shard.shards(3, 8).toSeq
    shards.size shouldBe 8
    shards.sum shouldBe 3
  }

  "Shard.shard" should "evenly distribute value 5 over 10" in {
    Shard.shard(5, 0, 10) shouldBe Shard(0, 1)
    Shard.shard(5, 1, 10) shouldBe Shard(1, 0)
    Shard.shard(5, 2, 10) shouldBe Shard(1, 1)
    Shard.shard(5, 3, 10) shouldBe Shard(2, 0)
    Shard.shard(5, 4, 10) shouldBe Shard(2, 1)
    Shard.shard(5, 5, 10) shouldBe Shard(3, 0)
    Shard.shard(5, 6, 10) shouldBe Shard(3, 1)
    Shard.shard(5, 7, 10) shouldBe Shard(4, 0)
    Shard.shard(5, 8, 10) shouldBe Shard(4, 1)
    Shard.shard(5, 9, 10) shouldBe Shard(5, 0)
  }

  it should "evenly distribute value 3 over 8" in {
    Shard.shard(3, 0, 8) shouldBe Shard(0, 1)
    Shard.shard(3, 1, 8) shouldBe Shard(1, 0)
    Shard.shard(3, 2, 8) shouldBe Shard(1, 1)
    Shard.shard(3, 3, 8) shouldBe Shard(2, 0)
    Shard.shard(3, 4, 8) shouldBe Shard(2, 0)
    Shard.shard(3, 5, 8) shouldBe Shard(2, 1)
    Shard.shard(3, 6, 8) shouldBe Shard(3, 0)
    Shard.shard(3, 7, 8) shouldBe Shard(3, 0)
  }

  private def forAllPairs(min: Int, max: Int)(f: (Int, Int) => Unit) =
    forAll(Gen.choose(min, max), Gen.choose(min, max)) { (a, b) =>
      whenever(a >= min && a <= max && b >= min && b <= max) {
        f(a, b)
      }
    }

  it should "produce the same results as Shard.shards" in {
    forAllPairs(1, 100) { case (totalValue, totalCount) =>
      val shards = Shard.shards(totalValue, totalCount).toArray
      for (index <- 0 until totalCount) {
        Shard.shard(totalValue, index, totalCount).length shouldBe shards(index)
      }
    }
  }

  it should "shard all values" in {
    forAllPairs(1, 100) { case (totalValue, totalCount) =>
      (0 until totalCount).map(index => Shard.shard(totalValue, index, totalCount).length).sum shouldBe totalValue
    }
  }

  it should "properly compute offsets" in {
    forAllPairs(1, 100) { case (totalValue, totalCount) =>
      var previousOffset = 0
      var previousLength = 0
      for {
        index <- 0 until totalCount
      } {
        val shard = Shard.shard(totalValue, index, totalCount)
        shard.offset shouldBe previousOffset + previousLength
        previousOffset = shard.offset
        previousLength = shard.length
      }
    }
  }
}
