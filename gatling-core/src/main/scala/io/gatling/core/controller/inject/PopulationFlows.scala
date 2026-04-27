/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

object PopulationFlows {
  final case class Node[K, T](key: K, value: T, childrenSequences: List[List[PopulationFlows.Node[K, T]]])

  final case class Flow[K, T](value: T, blockedBy: Set[K])

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def leavesKeys[K, T](node: Node[K, T]): List[K] =
    if (node.childrenSequences.isEmpty) {
      node.key :: Nil
    } else {
      for {
        child <- node.childrenSequences.lastOption.getOrElse(Nil)
        key <- leavesKeys(child)
      } yield key
    }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def flows[K, T](node: PopulationFlows.Node[K, T], parentBlockers: Set[K]): List[Flow[K, T]] = {
    val childrenFlows: List[Flow[K, T]] = {
      val it = node.childrenSequences.iterator
      if (it.isEmpty) {
        Nil
      } else {
        var previousSequence: List[Node[K, T]] = it.next()
        // first sequence is blocked by node
        var acc: List[Flow[K, T]] = previousSequence.flatMap(child => flows(child, Set(node.key)))

        while (it.hasNext) {
          val sequence = it.next()
          // next sequences are blocked by previous sequence's leaves
          val blockers = previousSequence.flatMap(leavesKeys).toSet
          acc ++= sequence.flatMap(flows(_, blockers))
          previousSequence = sequence
        }

        acc
      }
    }

    // node is blocked by parent
    Flow(node.value, parentBlockers) +: childrenFlows
  }

  def fromNodes[K, T](rootNodes: List[PopulationFlows.Node[K, T]]): PopulationFlows[K, T] =
    PopulationFlows(rootNodes.flatMap(flows(_, Set.empty)))
}

final case class PopulationFlows[K, T](locks: List[PopulationFlows.Flow[K, T]]) {
  def remove(key: K): PopulationFlows[K, T] =
    PopulationFlows(locks.map(lock => lock.copy(blockedBy = lock.blockedBy - key)))

  def extractReady: (List[T], PopulationFlows[K, T]) = {
    val (emptyLocks, nonEmptyLocks) = locks.partition(_.blockedBy.isEmpty)
    (emptyLocks.map(_.value), PopulationFlows(nonEmptyLocks))
  }

  def isEmpty: Boolean = locks.isEmpty
}
