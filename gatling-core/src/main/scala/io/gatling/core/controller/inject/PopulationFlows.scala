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
  final case class TopDownNode[K, T](key: K, value: T, childrenSequences: List[List[PopulationFlows.TopDownNode[K, T]]])

  final case class BottomUpNode[K, T](value: T, blockedBy: Set[K])

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def leavesKeys[K, T](topDownNode: TopDownNode[K, T]): List[K] =
    if (topDownNode.childrenSequences.isEmpty) {
      topDownNode.key :: Nil
    } else {
      for {
        child <- topDownNode.childrenSequences.lastOption.getOrElse(Nil)
        key <- leavesKeys(child)
      } yield key
    }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def flows[K, T](topDownNode: PopulationFlows.TopDownNode[K, T], parentBlockers: Set[K]): List[BottomUpNode[K, T]] = {
    val childrenBottomUpNodes: List[BottomUpNode[K, T]] = {
      val it = topDownNode.childrenSequences.iterator
      if (it.isEmpty) {
        Nil
      } else {
        var previousSequence: List[TopDownNode[K, T]] = it.next()
        // the first child sequence is blocked by the node itself
        var acc: List[BottomUpNode[K, T]] = previousSequence.flatMap(child => flows(child, Set(topDownNode.key)))

        while (it.hasNext) {
          val sequence = it.next()
          // the next sequences are blocked by the previous sequence's leaves
          val blockers = previousSequence.flatMap(leavesKeys).toSet
          acc ++= sequence.flatMap(flows(_, blockers))
          previousSequence = sequence
        }

        acc
      }
    }

    // the node is blocked by its parents
    BottomUpNode(topDownNode.value, parentBlockers) +: childrenBottomUpNodes
  }

  def fromTopDownNodes[K, T](rootTopDownNodes: List[PopulationFlows.TopDownNode[K, T]]): PopulationFlows[K, T] =
    PopulationFlows(rootTopDownNodes.flatMap(flows(_, Set.empty)))
}

final case class PopulationFlows[K, T](bottomUpNodes: List[PopulationFlows.BottomUpNode[K, T]]) {
  def remove(key: K): PopulationFlows[K, T] =
    PopulationFlows(bottomUpNodes.map(node => node.copy(blockedBy = node.blockedBy - key)))

  def unblocked: (List[T], PopulationFlows[K, T]) = {
    val (unblockedNodes, blockedNodes) = bottomUpNodes.partition(_.blockedBy.isEmpty)
    (unblockedNodes.map(_.value), PopulationFlows(blockedNodes))
  }

  def isEmpty: Boolean = bottomUpNodes.isEmpty
}
