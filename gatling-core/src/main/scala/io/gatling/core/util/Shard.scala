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

import scala.collection.AbstractIterator

object Shard {

  private[this] def sumFromZero(total: Long, buckets: Int, bucketNumber: Int): Long =
    if (bucketNumber == -1) {
      0L
    } else if (bucketNumber == buckets - 1) {
      // because of rounding, we might be one off on last bucket
      total
    } else {
      // +1 is because we want a non zero value in first bucket
      math.ceil(total.toDouble / buckets * (bucketNumber + 1)).toLong
    }

  def shard(total: Long, bucketNumber: Int, buckets: Int): Shard = {
    require(bucketNumber < buckets, s"bucketNumber=$bucketNumber should be less than buckets=$buckets")
    val offset = sumFromZero(total, buckets, bucketNumber - 1)
    val value = sumFromZero(total, buckets, bucketNumber) - offset
    Shard(offset.toInt, value.toInt)
  }

  def shards(total: Long, buckets: Int): Iterator[Long] =
    new AbstractIterator[Long] {
      private[this] var currentIndex = 0
      private[this] var previousSumFromZero = 0L

      override def hasNext: Boolean = currentIndex < buckets

      override def next(): Long = {
        val newSumFromZero = sumFromZero(total, buckets, currentIndex)
        val res = newSumFromZero - previousSumFromZero
        currentIndex += 1
        previousSumFromZero = newSumFromZero
        res
      }
    }
}

final case class Shard(offset: Int, length: Int)
