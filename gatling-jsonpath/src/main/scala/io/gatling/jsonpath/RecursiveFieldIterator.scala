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

package io.gatling.jsonpath

import java.{ util => ju }

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType.{ ARRAY, OBJECT }

sealed trait VisitedIterator {
  def hasNext: Boolean
}
final case class VisitedObject(it: ju.Iterator[ju.Map.Entry[String, JsonNode]]) extends VisitedIterator {
  override def hasNext: Boolean = it.hasNext
}
final case class VisitedArray(it: ju.Iterator[JsonNode]) extends VisitedIterator {
  override def hasNext: Boolean = it.hasNext
}

/**
 * Collect all first nodes in a branch with a given name
 * @param root the tree root
 * @param name the searched name
 *
 *  Originally contributed by Nicolas Rémond.
 */
class RecursiveFieldIterator(root: JsonNode, name: String) extends RecursiveIterator[VisitedIterator](root) {

  override def visit(t: VisitedIterator): Unit = t match {
    case VisitedObject(it) => visitObject(it)
    case VisitedArray(it)  => visitArray(it)
  }

  private def visitObject(it: ju.Iterator[ju.Map.Entry[String, JsonNode]]): Unit = {
    while (it.hasNext && !pause) {
      val e = it.next()
      if (e.getKey == name) {
        nextNode = e.getValue
        pause = true
      } else {
        visitNode(e.getValue)
      }
    }
    if (!pause) {
      stack = stack.tail
    }
  }

  private def visitArray(it: ju.Iterator[JsonNode]): Unit = {
    while (it.hasNext && !pause) {
      visitNode(it.next())
    }
    if (!pause) {
      stack = stack.tail
    }
  }

  protected def visitNode(node: JsonNode): Unit =
    node.getNodeType match {
      case OBJECT =>
        val it = node.fields
        stack = VisitedObject(it) :: stack
        visitObject(it)
      case ARRAY =>
        val it = node.elements
        stack = VisitedArray(it) :: stack
        visitArray(it)
      case _ =>
    }
}
