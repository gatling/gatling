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

package io.gatling.javaapi.core.internal

import java.{ util => ju }
import java.time.Duration
import java.util.{ function => juf }

import scala.collection.immutable.{ ArraySeq, Seq }
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._
import scala.jdk.DurationConverters._
import scala.jdk.FunctionConverters._

object Converters {

  def toScalaFunction[T, R](f: juf.Function[T, R]): Function[T, R] = f.asScala

  def toScalaDuration(duration: Duration): FiniteDuration = duration.toScala

  def toScalaSeq[T](array: Array[T]): Seq[T] = ArraySeq.unsafeWrapArray(array)

  def toScalaSeq[T](list: ju.List[T]): Seq[T] = list.asScala.toSeq

  def toJavaList[T](seq: scala.collection.Seq[T]): ju.List[T] = new ju.ArrayList(seq.asJavaCollection)

  def toJavaList[T](t: (T, T)): ju.List[T] = ju.Arrays.asList(t._1, t._2)
  def toJavaList[T](t: (T, T, T)): ju.List[T] = ju.Arrays.asList(t._1, t._2, t._3)
  def toJavaList[T](t: (T, T, T, T)): ju.List[T] = ju.Arrays.asList(t._1, t._2, t._3, t._4)
  def toJavaList[T](t: (T, T, T, T, T)): ju.List[T] = ju.Arrays.asList(t._1, t._2, t._3, t._4, t._5)
  def toJavaList[T](t: (T, T, T, T, T, T)): ju.List[T] = ju.Arrays.asList(t._1, t._2, t._3, t._4, t._5, t._6)
  def toJavaList[T](t: (T, T, T, T, T, T, T)): ju.List[T] = ju.Arrays.asList(t._1, t._2, t._3, t._4, t._5, t._6, t._7)
  def toJavaList[T](t: (T, T, T, T, T, T, T, T)): ju.List[T] = ju.Arrays.asList(t._1, t._2, t._3, t._4, t._5, t._6, t._7, t._8)

  def toScalaSet[T](set: ju.Set[T]): Set[T] = set.asScala.toSet

  def toJavaSet[T](set: scala.collection.Set[T]): ju.Set[T] = set.asJava

  def toJavaMap[T](map: scala.collection.Map[String, T]): ju.Map[String, T] = map.asJava

  def toScalaMap[K, V](map: ju.Map[K, V]): Map[K, V] = map.asScala.toMap

  def toScalaTuple2Seq(list: ju.List[ju.Map.Entry[String, Object]]): Seq[(String, Object)] =
    list.asScala.map(entry => entry.getKey -> entry.getValue).toSeq
}
