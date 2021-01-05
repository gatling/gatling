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

import scala.collection.AbstractIterator

import com.fasterxml.jackson.databind.JsonNode

/**
 * Originally contributed by Nicolas Rémond.
 */
abstract class RecursiveIterator[T](root: JsonNode) extends AbstractIterator[JsonNode] {

  protected var nextNode: JsonNode = _
  protected var finished: Boolean = _
  protected var pause: Boolean = _
  protected var stack: List[T] = _

  protected def visitNode(node: JsonNode): Unit

  protected def visit(t: T): Unit

  override def hasNext: Boolean =
    (nextNode != null && !finished) || {
      pause = false
      if (stack == null) {
        // first access
        stack = Nil
        visitNode(root)
      } else {
        // resuming
        while (!pause && stack.nonEmpty) {
          visit(stack.head)
        }
      }

      finished = nextNode == null
      !finished
    }

  override def next(): JsonNode =
    if (finished) {
      throw new UnsupportedOperationException("Can't call next on empty Iterator")
    } else if (nextNode == null) {
      throw new UnsupportedOperationException("Can't call next without calling hasNext first")
    } else {
      val consumed = nextNode
      nextNode = null
      consumed
    }
}
