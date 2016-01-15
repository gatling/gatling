/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
    if (nodeCount == 1) {
      Shard(0, total)
    } else {
      val largeBucketCount = total % nodeCount
      val smallBucketSize = total / nodeCount
      val largeBucketSize = smallBucketSize + 1

      // large buckets first
      if (nodeId < largeBucketCount)
        Shard(nodeId * largeBucketSize, largeBucketSize)
      else
        Shard(largeBucketCount * largeBucketSize + (nodeId - largeBucketCount) * smallBucketSize, smallBucketSize)
    }

  private[this] def interleave(largestCount: Long, largestValue: Long, smallestCount: Long, smallestValue: Long, totalCount: Int): Iterator[Long] = {
    // more large than small
    val largeForSmallRatio = (largestCount / smallestCount).toInt
    val rest = (totalCount - smallestCount * (largeForSmallRatio + 1)).toInt

    Iterator.fill(smallestCount.toInt)(Iterator.single(smallestValue) ++ Iterator.fill(largeForSmallRatio)(largestValue)).flatten ++
      Iterator.fill(rest)(largestValue)
  }

  def shards(total: Long, nodeCount: Int): Iterator[Long] = {

    val smallBucketSize = total / nodeCount
    val largeBucketSize = smallBucketSize + 1

    val largeBucketCount = total % nodeCount
    val smallBucketCount = nodeCount - largeBucketCount

    if (largeBucketCount == 0) {
      Iterator.fill(smallBucketCount.toInt)(smallBucketSize)

    } else if (smallBucketCount == 0) {
      Iterator.fill(largeBucketCount.toInt)(largeBucketSize)

    } else if (smallBucketCount > largeBucketCount) {
      interleave(smallBucketCount, smallBucketSize, largeBucketCount, largeBucketSize, nodeCount)

    } else {
      interleave(largeBucketCount, largeBucketSize, smallBucketCount, smallBucketSize, nodeCount)
    }
  }
}

case class Shard(offset: Int, length: Int)
