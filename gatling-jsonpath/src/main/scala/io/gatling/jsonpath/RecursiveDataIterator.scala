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

/**
 * Collect all nodes data (objects and leaves)
 * @param root the tree root
 *
 * Originally contributed by Nicolas Rémond.
 */
class RecursiveDataIterator(root: JsonNode) extends RecursiveIterator[ju.Iterator[JsonNode]](root) {

  override protected def visit(it: ju.Iterator[JsonNode]): Unit = {
    while (it.hasNext && !pause) {
      visitNode(it.next())
    }
    if (!pause) {
      stack = stack.tail
    }
  }

  override protected def visitNode(node: JsonNode): Unit =
    node.getNodeType match {
      case OBJECT =>
        if (node.size > 0) {
          // only non empty objects
          val it = node.elements
          stack = it :: stack
          nextNode = node
          pause = true
        }

      case ARRAY =>
        val it = node.elements
        stack = it :: stack
        visit(it)
      case _ =>
        nextNode = node
        pause = true
    }
}
