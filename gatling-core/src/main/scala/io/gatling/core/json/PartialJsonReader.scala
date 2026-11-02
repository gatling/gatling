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

import com.fasterxml.jackson.core.{ JsonParser, JsonToken }
import com.fasterxml.jackson.databind.{ JsonNode, ObjectMapper }
import com.fasterxml.jackson.databind.node.{ ArrayNode, NullNode, ObjectNode }

/**
 * Builds the partial tree of a JSON document needed by a [[JsonSubset]], in a single streaming pass:
 *   - only the values at the end of the paths are read as whole subtrees, all the other values are skipped without being decoded;
 *   - objects and arrays along the paths only contain the members the paths go through, array elements that are skipped before a needed one are replaced with
 *     null so that indexes are preserved;
 *   - when all the elements of an array are needed, they're all kept, each one reduced to what the paths read in it;
 *   - the parsing stops as soon as all the values have been read, an array whose elements are all needed counting as read once it's closed, so the rest of the
 *     document is neither read nor validated;
 *   - if a key is duplicated, the first occurrence wins.
 */
private[gatling] object PartialJsonReader {
  def read(parser: JsonParser, subset: JsonSubset, objectMapper: ObjectMapper): JsonNode =
    if (parser.nextToken() == null) {
      throw new IllegalArgumentException("No content to map due to end-of-input")
    } else {
      new Reader(parser, objectMapper, subset.leafCount).read(subset.root, counting = true)
    }

  private final class Reader(parser: JsonParser, objectMapper: ObjectMapper, private var remaining: Int) {
    private val nodeFactory = objectMapper.getNodeFactory

    // the parser is on the first token of the value
    // counting: if the values read count for the early exit, ie they're not in an array whose elements are all needed, as they can't be counted in advance
    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def read(node: JsonSubset.Node, counting: Boolean): JsonNode =
      if (node.leaf) {
        val value: JsonNode = objectMapper.readTree(parser)
        if (counting) {
          remaining -= 1
        }
        value
      } else {
        parser.currentToken match {
          case JsonToken.START_OBJECT => readObject(node, counting)
          case JsonToken.START_ARRAY  =>
            node.allElements match {
              case Some(all) => readAllElements(node, all, counting)
              case _         => readArray(node, counting)
            }
          case _ => objectMapper.readTree(parser) // a scalar where the paths expect a container: they won't find anything
        }
      }

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    private def readObject(node: JsonSubset.Node, counting: Boolean): ObjectNode = {
      val obj = nodeFactory.objectNode
      while (remaining > 0 && parser.nextToken() == JsonToken.FIELD_NAME) {
        val name = parser.currentName
        parser.nextToken()
        node.fields.get(name) match {
          case Some(child) if !obj.has(name) => obj.set[JsonNode](name, read(child, counting))
          case _                             => parser.skipChildren()
        }
      }
      obj
    }

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    private def readArray(node: JsonSubset.Node, counting: Boolean): ArrayNode = {
      val array = nodeFactory.arrayNode
      var index = 0
      while (remaining > 0 && parser.nextToken() != JsonToken.END_ARRAY) {
        if (index <= node.maxIndex) {
          node.indexes.get(index) match {
            case Some(child) => array.add(read(child, counting))
            case _           =>
              parser.skipChildren()
              array.add(NullNode.instance)
          }
        } else {
          parser.skipChildren()
        }
        index += 1
      }
      array
    }

    // the number of elements isn't known in advance, so the array has to be read until its end before the values it contains count as read
    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    private def readAllElements(node: JsonSubset.Node, all: JsonSubset.Node, counting: Boolean): ArrayNode = {
      val array = nodeFactory.arrayNode
      var index = 0
      while (parser.nextToken() != JsonToken.END_ARRAY) {
        array.add(read(node.indexesWithAllElements.getOrElse(index, all), counting = false))
        index += 1
      }
      if (counting) {
        remaining -= node.leafCount
      }
      array
    }
  }
}
