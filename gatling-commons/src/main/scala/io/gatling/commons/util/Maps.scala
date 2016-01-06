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
package io.gatling.commons.util

import scala.collection.immutable.ListMap
import scala.collection.mutable

object Maps {

  trait Merger[T] {
    def copy(value: T): T
    def merge(left: T, right: T): T
  }

  implicit val LongMerger = new Merger[Long] {
    override def copy(value: Long): Long = value
    override def merge(left: Long, right: Long): Long = left + right
  }

  implicit def seqMerger[T] = new Merger[Seq[T]] {
    override def copy(value: Seq[T]): Seq[T] = value
    override def merge(left: Seq[T], right: Seq[T]): Seq[T] = left ++ right
  }

  implicit def mapMerger[K, V](implicit merger: Merger[V]) = new Merger[Map[K, V]] {
    override def copy(value: Map[K, V]): Map[K, V] = value.forceMapValues(merger.copy)
    override def merge(left: Map[K, V], right: Map[K, V]): Map[K, V] =
      (left.keySet ++ right.keySet).map { key =>

        val value = left.get(key) match {
          case None => merger.copy(right(key))
          case Some(leftValue) => right.get(key) match {
            case None             => leftValue
            case Some(rightValue) => merger.merge(leftValue, rightValue)
          }
        }

        key -> value
      }.toMap
  }

  implicit class PimpedMap[K, V](val map: Map[K, V]) extends AnyVal {

    def forceMapValues[V2](f: V => V2) = map.mapValues(f).view.force

    /**
     * Merge with another map. Left is this map and right the other one.
     *
     * @param other the map to merge into this map
     * @return a merged map
     */
    def mergeWith(other: Map[K, V])(implicit merger: Merger[V]): Map[K, V] =
      mapMerger[K, V].merge(map, other)
  }

  implicit class PimpedPairTraversableOnce[K, V](val iterable: TraversableOnce[(K, V)]) extends AnyVal {

    def groupByKey[K2](f: K => K2): mutable.Map[K2, mutable.ArrayBuffer[V]] = {
      val mm = new mutable.HashMap[K2, mutable.ArrayBuffer[V]]
      for {
        (k1, value) <- iterable
      } {
        val k2 = f(k1)
        if (mm.contains(k2)) {
          mm(k2) += value
        } else {
          val newEntry = new mutable.ArrayBuffer[V](1)
          newEntry += value
          mm.update(k2, newEntry)
        }
      }
      mm
    }

    def sortByKey(implicit odering: Ordering[K]): Map[K, V] =
      ListMap(iterable.toSeq.sortBy(_._1): _*)

    def sortBy[T](f: K => T)(implicit odering: Ordering[T]): Map[K, V] =
      ListMap(iterable.toSeq.sortBy(t => f(t._1)): _*)
  }
}
