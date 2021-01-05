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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ BooleanNode, DoubleNode, LongNode, NullNode, TextNode }

/**
 * Originally contributed by Nicolas Rémond.
 */
object AST {
  sealed trait AstToken extends Product with Serializable
  sealed trait PathToken extends AstToken

  sealed trait FieldAccessor extends PathToken
  case object RootNode extends FieldAccessor
  final case class Field(name: String) extends FieldAccessor
  final case class RecursiveField(name: String) extends FieldAccessor
  final case class MultiField(names: List[String]) extends FieldAccessor
  final case object AnyField extends FieldAccessor
  final case object RecursiveAnyField extends FieldAccessor

  sealed trait ArrayAccessor extends PathToken

  /**
   * Slicing of an array, indices start at zero
   *
   * @param start is the first item that you want (of course)
   * @param stop is the first item that you do not want
   * @param step, being positive or negative, defines whether you are moving
   */
  final case class ArraySlice(start: Option[Int], stop: Option[Int], step: Int) extends ArrayAccessor
  object ArraySlice {
    val All: ArraySlice = ArraySlice(None, None, 1)
  }
  final case class ArrayRandomAccess(indices: List[Int]) extends ArrayAccessor

  // JsonPath Filter AST //////////////////////////////////////////////

  final case object CurrentNode extends PathToken
  sealed trait FilterValue extends AstToken

  object FilterDirectValue {
    def long(value: Long): FilterDirectValue = FilterDirectValue(new LongNode(value))
    def double(value: Double): FilterDirectValue = FilterDirectValue(new DoubleNode(value))
    val True: FilterDirectValue = FilterDirectValue(BooleanNode.TRUE)
    val False: FilterDirectValue = FilterDirectValue(BooleanNode.FALSE)
    def string(value: String): FilterDirectValue = FilterDirectValue(new TextNode(value))
    val Null: FilterDirectValue = FilterDirectValue(NullNode.instance)
  }

  final case class FilterDirectValue(node: JsonNode) extends FilterValue

  final case class SubQuery(path: List[PathToken]) extends FilterValue

  sealed trait FilterToken extends PathToken
  final case class HasFilter(query: SubQuery) extends FilterToken
  final case class ComparisonFilter(operator: ComparisonOperator, lhs: FilterValue, rhs: FilterValue) extends FilterToken
  final case class BooleanFilter(operator: BinaryBooleanOperator, lhs: FilterToken, rhs: FilterToken) extends FilterToken

  final case class RecursiveFilterToken(filter: FilterToken) extends PathToken
}
