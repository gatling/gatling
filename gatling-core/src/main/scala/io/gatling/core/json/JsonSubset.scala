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

package io.gatling.core.json

import scala.annotation.tailrec

import io.gatling.core.check.PreparedSubset

/**
 * The parts of a JSON document some checks read, as a tree of field names, array indexes and all the elements of arrays. The value at the end of each path is
 * needed as a whole, for example `$.meta` needs the whole `meta` object.
 *
 * Only built from paths that follow field names, non-negative array indexes and all the elements of arrays from the root, eg `$.meta.code`, `data[0].id` or
 * `$.data[*].id`. The result of such a path only depends on the values found along it, so evaluating it against the partial tree built by [[PartialJsonReader]]
 * gives the same result as against the full tree.
 */
final case class JsonSubset private (root: JsonSubset.Node) extends PreparedSubset {
  def ++(other: JsonSubset): JsonSubset = JsonSubset(root.merge(other.root))

  /** The number of values to read before the parsing can stop, an array whose elements are all needed counting as a single value. */
  private[json] def leafCount: Int = root.leafCount
}

object JsonSubset {
  sealed trait Segment extends Product with Serializable
  object Segment {
    final case class Field(name: String) extends Segment
    final case class Index(index: Int) extends Segment
    case object AllElements extends Segment
  }

  /**
   * @param allElements
   *   what's needed in every element of the array, if any
   */
  private[json] final case class Node(leaf: Boolean, fields: Map[String, Node], indexes: Map[Int, Node], allElements: Option[Node]) {
    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def merge(other: Node): Node =
      if (leaf || other.leaf) {
        Node.Leaf
      } else {
        val mergedAllElements = (allElements.toList ::: other.allElements.toList).reduceOption(_.merge(_))
        Node(
          leaf = false,
          fields = mergeChildren(fields, other.fields),
          // when all the elements are needed as a whole, there's nothing more to read in some of them
          indexes = if (mergedAllElements.exists(_.leaf)) Map.empty else mergeChildren(indexes, other.indexes),
          allElements = mergedAllElements
        )
      }

    private def mergeChildren[K](left: Map[K, Node], right: Map[K, Node]): Map[K, Node] =
      right.foldLeft(left) { case (acc, (key, node)) =>
        acc.updated(key, acc.get(key).fold(node)(_.merge(node)))
      }

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def leafCount: Int =
      if (leaf) 1
      else if (allElements.isDefined) 1 + fields.values.map(_.leafCount).sum
      else fields.values.map(_.leafCount).sum + indexes.values.map(_.leafCount).sum

    val maxIndex: Int = if (indexes.isEmpty) -1 else indexes.keys.max

    /** When all the elements of the array are needed, what's needed in the ones at some indexes, on top of [[allElements]]. */
    val indexesWithAllElements: Map[Int, Node] =
      allElements.fold(Map.empty[Int, Node])(all => indexes.map { case (index, node) => index -> node.merge(all) })
  }

  private[json] object Node {
    val Leaf: Node = Node(leaf = true, Map.empty, Map.empty, None)
    val Empty: Node = Node(leaf = false, Map.empty, Map.empty, None)
  }

  /**
   * @return
   *   None for the empty path, ie the whole document
   */
  def apply(path: List[Segment]): Option[JsonSubset] = {
    @tailrec
    def build(reversedPath: List[Segment], node: Node): Node =
      reversedPath match {
        case Segment.Field(name) :: tail  => build(tail, Node.Empty.copy(fields = Map(name -> node)))
        case Segment.Index(index) :: tail => build(tail, Node.Empty.copy(indexes = Map(index -> node)))
        case Segment.AllElements :: tail  => build(tail, Node.Empty.copy(allElements = Some(node)))
        case _                            => node
      }

    if (path.isEmpty) None else Some(JsonSubset(build(path.reverse, Node.Leaf)))
  }
}
