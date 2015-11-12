/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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

object Shard {
  def shard(total: Int, nodeId: Int, nodeCount: Int): Shard =
    if (nodeCount == 1)
      Shard(0, total)
    else {
      val largeBucketCount = total % nodeCount
      val smallBucketSize = total / nodeCount
      val largeBucketSize = smallBucketSize + 1

      // large buckets first
      if (nodeId < largeBucketCount)
        Shard(nodeId * largeBucketSize, largeBucketSize)
      else
        Shard(largeBucketCount * largeBucketSize + (nodeId - largeBucketCount) * smallBucketSize, smallBucketSize)
    }

  def shards(total: Long, nodeCount: Int): Iterator[Long] = {
    val largeBucketCount = total % nodeCount
    val smallBucketSize = total / nodeCount
    val largeBucketSize = smallBucketSize + 1

    Iterator.fill(largeBucketCount.toInt)(largeBucketSize) ++ Iterator.fill((total - largeBucketCount).toInt)(smallBucketSize)
  }
}

case class Shard(offset: Int, length: Int)
