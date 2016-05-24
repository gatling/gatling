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

  private[this] def pick(countA: Long, valueA: Long, countB: Long, valueB: Long, nodeId: Int): Shard = {
    // assumes countA > countB
    // pattern = (A... ratio times) then (B once), order depend on valueA >= valueB
    val ratioAforB = (countA / countB).toInt
    val patternLength = ratioAforB + 1
    val numberOfFullPatterns = nodeId / patternLength
    val modulo = nodeId % patternLength
    val patternValue = ratioAforB * valueA + valueB
    val fullPatternsValue = numberOfFullPatterns * patternValue

    if (valueA >= valueB) {
      // A first
      if (modulo == ratioAforB) {
        // (A, ..., A, B)
        Shard((fullPatternsValue + patternValue - valueB).toInt, valueB.toInt)
      } else {
        // (A, ..., A)
        Shard((fullPatternsValue + modulo * valueA).toInt, valueA.toInt)
      }

    } else {
      // B first
      if (modulo == 0) {
        // next value is B, except if we've reached countB
        if (numberOfFullPatterns < countB) {
          // (... A)(B)
          Shard(fullPatternsValue.toInt, valueB.toInt)
        } else {
          // (... A)(A)
          Shard(fullPatternsValue.toInt, valueA.toInt)
        }

      } else {
        // next value is B, except if we've reached countB
        if (numberOfFullPatterns < countB) {
          // (B, A, ..., A)
          Shard((fullPatternsValue + valueB + (modulo - 1) * valueA).toInt, valueA.toInt)

        } else {
          // (A, ..., A)
          Shard((fullPatternsValue + modulo * valueA).toInt, valueA.toInt)
        }
      }
    }
  }

  def shard(total: Int, nodeId: Int, nodeCount: Int): Shard =
    if (nodeCount == 1) {
      Shard(0, total)
    } else {
      val largeBucketCount = total % nodeCount
      val smallBucketCount = nodeCount - largeBucketCount
      val smallBucketSize = total / nodeCount
      val largeBucketSize = smallBucketSize + 1

      if (smallBucketCount == 0) {
        Shard(nodeId * largeBucketSize, largeBucketSize)

      } else if (largeBucketCount == 0) {
        Shard(nodeId * smallBucketSize, smallBucketSize)

      } else if (largeBucketCount > smallBucketCount) {
        pick(largeBucketCount, largeBucketSize, smallBucketCount, smallBucketSize, nodeId)

      } else {
        pick(smallBucketCount, smallBucketSize, largeBucketCount, largeBucketSize, nodeId)
      }
    }

  private[this] def interleave(countA: Long, valueA: Long, countB: Long, valueB: Long, totalCount: Int): Iterator[Long] = {
    // assumes countA > countB
    val ratioAforB = (countA / countB).toInt
    val rest = (totalCount - countB * (ratioAforB + 1)).toInt

    // largest values first
    val pattern =
      if (valueA >= valueB) {
        () => Iterator.fill(ratioAforB)(valueA) ++ Iterator.single(valueB)
      } else {
        () => Iterator.single(valueB) ++ Iterator.fill(ratioAforB)(valueA)
      }

    Iterator.fill(countB.toInt)(pattern()).flatten ++ Iterator.fill(rest)(valueA)
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
