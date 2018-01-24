/**
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

  private[this] def pick(countA: Long, valueA: Long, countB: Long, valueB: Long, index: Int): Shard = {
    // assumes countA > countB
    // pattern = (A... ratio times) then (B once), order depend on valueA >= valueB
    val ratioAforB = (countA / countB).toInt
    val patternLength = ratioAforB + 1
    val numberOfFullPatterns = math.min(index / patternLength, countB)
    val patternValue = ratioAforB * valueA + valueB
    val fullPatternsValue = numberOfFullPatterns * patternValue

    if (numberOfFullPatterns == countB) {
      // we've reached the number of full patterns, next values will only be valueA (the one with the greatest count)
      Shard((fullPatternsValue + (index - countB * patternLength) * valueA).toInt, valueA.toInt)

    } else {
      val modulo = index % patternLength

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
  }

  def shard(totalValue: Int, index: Int, totalCount: Int): Shard =
    if (totalCount == 1) {
      Shard(0, totalValue)
    } else {
      val largeCount = totalValue % totalCount
      val smallCount = totalCount - largeCount
      val smallValue = totalValue / totalCount
      val largeValue = smallValue + 1

      if (smallCount == 0) {
        Shard(index * largeValue, largeValue)

      } else if (largeCount == 0) {
        Shard(index * smallValue, smallValue)

      } else if (largeCount > smallCount) {
        pick(largeCount, largeValue, smallCount, smallValue, index)

      } else {
        pick(smallCount, smallValue, largeCount, largeValue, index)
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

    // countB * (valueB + (countA / countB).toInt * valueA) + (totalCount - countB * (ratioAforB + 1)).toInt * valueA
    Iterator.fill(countB.toInt)(pattern()).flatten ++ Iterator.fill(rest)(valueA)
  }

  def shards(totalValue: Long, totalCount: Int): Iterator[Long] = {

    val smallValue = totalValue / totalCount
    val largeValue = smallValue + 1

    val largeBucketCount = totalValue % totalCount
    val smallBucketCount = totalCount - largeBucketCount

    if (largeBucketCount == 0) {
      Iterator.fill(smallBucketCount.toInt)(smallValue)

    } else if (smallBucketCount == 0) {
      Iterator.fill(largeBucketCount.toInt)(largeValue)

    } else if (smallBucketCount > largeBucketCount) {
      interleave(smallBucketCount, smallValue, largeBucketCount, largeValue, totalCount)

    } else {
      interleave(largeBucketCount, largeValue, smallBucketCount, smallValue, totalCount)
    }
  }
}

case class Shard(offset: Int, length: Int)
