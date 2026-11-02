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

package io.gatling.core.check.jmespath

import java.{ util => ju }
import java.lang.reflect.Field

import scala.jdk.CollectionConverters._
import scala.util.Try
import scala.util.control.NonFatal

import io.gatling.core.json.JsonSubset

import io.burt.jmespath.Expression
import io.burt.jmespath.node.{ CurrentNode, IndexNode, ProjectionNode, PropertyNode, SequenceNode }

/**
 * Computes the [[JsonSubset]] a compiled JMESPath expression reads, when it only follows properties, non-negative indexes and list projections, eg `meta.code`,
 * `"foo-bar".code`, `data[0].id` or `data[*].id`.
 *
 * jmespath-java doesn't expose its AST nodes' properties, so they're read with reflection. Anything unexpected, such as another node type or a reflection
 * failure after an upgrade, results in None, ie the whole document is needed.
 */
private[jmespath] object JmesPathSubsets {
  private def privateField(clazz: Class[_], name: String): Option[Field] =
    Try {
      val field = clazz.getDeclaredField(name)
      field.setAccessible(true)
      field
    }.toOption

  private final case class Fields(sequenceNodes: Field, propertyName: Field, index: Field, projection: Field)

  private val fields: Option[Fields] =
    for {
      sequenceNodes <- privateField(classOf[SequenceNode[_]], "nodes")
      propertyName <- privateField(classOf[PropertyNode[_]], "rawPropertyName")
      index <- privateField(classOf[IndexNode[_]], "index")
      projection <- privateField(classOf[ProjectionNode[_]], "projection")
    } yield Fields(sequenceNodes, propertyName, index, projection)

  def apply(expression: Expression[_]): Option[JsonSubset] =
    fields.flatMap { case Fields(sequenceNodes, propertyName, index, projection) =>
      @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
      def segments(node: Any): Option[List[JsonSubset.Segment]] =
        node match {
          case _: CurrentNode[_]  => Some(Nil)
          case p: PropertyNode[_] =>
            propertyName.get(p) match {
              case name: String => Some(List(JsonSubset.Segment.Field(name)))
              case _            => None
            }
          case i: IndexNode[_] =>
            index.get(i) match {
              case n: Integer if n >= 0 => Some(List(JsonSubset.Segment.Index(n)))
              case _                    => None
            }
          // `[*]` on a list, applying the rest of the expression to each element, eg `data[*].id`
          case p: ProjectionNode[_] => segments(projection.get(p)).map(JsonSubset.Segment.AllElements :: _)
          // pipes are compiled into sequences too, so a projection must be the last part of a sequence:
          // in `data[*] | [0]`, `[0]` is applied to the result of the projection, not to each element
          case s: SequenceNode[_] =>
            sequenceNodes.get(s) match {
              case nodes: ju.List[_] =>
                nodes.asScala.foldLeft(Option(List.empty[JsonSubset.Segment])) { (acc, child) =>
                  for {
                    previous <- acc if !previous.contains(JsonSubset.Segment.AllElements)
                    next <- segments(child)
                  } yield previous ::: next
                }
              case _ => None
            }
          case _ => None
        }

      try {
        segments(expression).flatMap(JsonSubset(_))
      } catch {
        case NonFatal(_) => None
      }
    }
}
