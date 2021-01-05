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

import scala.jdk.CollectionConverters._
import scala.math.abs

import io.gatling.jsonpath.AST._

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType._

final case class JPError(reason: String)

/**
 * Originally contributed by Nicolas Rémond.
 */
object JsonPath {
  private val JsonPathParser = ThreadLocal.withInitial[GatlingElParser](() => new GatlingElParser)

  def compile(query: String): Either[JPError, JsonPath] =
    JsonPathParser.get.compile(query) match {
      case GatlingElParser.Success(q, _) => Right(new JsonPath(q))
      case ns: GatlingElParser.NoSuccess => Left(JPError(ns.msg))
    }

  def query(query: String, jsonObject: JsonNode): Either[JPError, Iterator[JsonNode]] =
    compile(query).map(_.query(jsonObject))
}

class JsonPath(path: List[PathToken]) {
  def query(jsonNode: JsonNode): Iterator[JsonNode] = new JsonPathWalker(jsonNode, path).walk()
}

class JsonPathWalker(rootNode: JsonNode, fullPath: List[PathToken]) {

  def walk(): Iterator[JsonNode] = walk(rootNode, fullPath)

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private[this] def walk(node: JsonNode, query: List[PathToken]): Iterator[JsonNode] =
    query match {
      case head :: tail => walk1(node, head, tail)
      case _            => Iterator.single(node)
    }

  private[this] def walk1(node: JsonNode, queryHead: PathToken, queryTail: List[PathToken]): Iterator[JsonNode] =
    queryHead match {
      case RootNode => walk(rootNode, queryTail)

      case CurrentNode => walk(node, queryTail)

      case Field(name) =>
        val child = node.get(name)
        if (child == null) Iterator.empty else walk(child, queryTail)

      case RecursiveField(name) => new RecursiveFieldIterator(node, name).flatMap(walk(_, queryTail))

      case MultiField(fieldNames) =>
        if (node.getNodeType == OBJECT) {
          fieldNames.iterator
            .flatMap { name =>
              val child = node.get(name)
              if (child == null) Iterator.empty else walk(child, queryTail)
            }
        } else {
          Iterator.empty
        }

      case AnyField =>
        if (node.getNodeType == OBJECT) {
          node.elements.asScala.flatMap(walk(_, queryTail))
        } else {
          Iterator.empty
        }

      case ArraySlice.All =>
        if (node.getNodeType == ARRAY) {
          node.elements.asScala.flatMap(walk(_, queryTail))
        } else {
          Iterator.empty
        }

      case ArraySlice(start, stop, step) =>
        if (node.getNodeType == ARRAY) {
          sliceArray(node, start, stop, step).flatMap(walk(_, queryTail))
        } else {
          Iterator.empty
        }

      case ArrayRandomAccess(indices) =>
        if (node.getNodeType == ARRAY) {
          indices.iterator
            .collect {
              case i if i >= 0 && i < node.size  => node.get(i)
              case i if i < 0 && i >= -node.size => node.get(i + node.size)
            }
            .flatMap(walk(_, queryTail))
        } else {
          Iterator.empty
        }

      case RecursiveFilterToken(filterToken) => new RecursiveDataIterator(node).flatMap(applyFilter(_, filterToken)).flatMap(walk(_, queryTail))

      case filterToken: FilterToken => applyFilter(node, filterToken).flatMap(walk(_, queryTail))

      case RecursiveAnyField => new RecursiveNodeIterator(node).flatMap(walk(_, queryTail))
    }

  private[this] def applyFilter(currentNode: JsonNode, filterToken: FilterToken): Iterator[JsonNode] = {

    def resolveSubQuery(node: JsonNode, subQuery: List[PathToken], nextOp: JsonNode => Boolean): Boolean = {
      val it = walk(node, subQuery)
      it.hasNext && nextOp(it.next())
    }

    def applyBinaryOpWithResolvedLeft(node: JsonNode, op: ComparisonOperator, lhsNode: JsonNode, rhs: FilterValue): Boolean =
      rhs match {
        case FilterDirectValue(valueNode) => op(lhsNode, valueNode)
        case SubQuery(subQuery)           => resolveSubQuery(node, subQuery, op(lhsNode, _))
      }

    def applyBinaryOp(node: JsonNode, op: ComparisonOperator, lhs: FilterValue, rhs: FilterValue): Boolean =
      lhs match {
        case FilterDirectValue(valueNode) => applyBinaryOpWithResolvedLeft(node, op, valueNode, rhs)
        case SubQuery(subQuery)           => resolveSubQuery(node, subQuery, applyBinaryOpWithResolvedLeft(node, op, _, rhs))
      }

    def elementsToFilter(node: JsonNode): Iterator[JsonNode] =
      node.getNodeType match {
        case ARRAY  => node.elements.asScala
        case OBJECT => Iterator.single(node)
        case _      => Iterator.empty
      }

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def evaluateFilter(node: JsonNode, filterToken: FilterToken): Boolean =
      filterToken match {
        case HasFilter(subQuery) =>
          walk(node, subQuery.path).hasNext

        case ComparisonFilter(op, lhs, rhs) =>
          applyBinaryOp(node, op, lhs, rhs)

        case BooleanFilter(op, filter1, filter2) =>
          val f1 = evaluateFilter(node, filter1)
          val f2 = evaluateFilter(node, filter2)
          op(f1, f2)
      }

    elementsToFilter(currentNode).filter(evaluateFilter(_, filterToken))
  }

  private[this] def sliceArray(array: JsonNode, start: Option[Int], stop: Option[Int], step: Int): Iterator[JsonNode] = {
    val size = array.size

    def lenRelative(x: Int) = if (x >= 0) x else size + x
    def stepRelative(x: Int) = if (step >= 0) x else -1 - x
    def relative(x: Int) = lenRelative(stepRelative(x))

    val absStart = start match {
      case Some(v) => relative(v)
      case _       => 0
    }
    val absEnd = stop match {
      case Some(v) => relative(v)
      case _       => size
    }
    val absStep = abs(step)

    val elements: Iterator[JsonNode] = if (step < 0) Iterator.range(array.size - 1, -1, -1).map(array.get) else array.elements.asScala
    val fromStartToEnd = elements.slice(absStart, absEnd)

    if (absStep == 1)
      fromStartToEnd
    else
      fromStartToEnd.grouped(absStep).map(_.head)
  }
}
