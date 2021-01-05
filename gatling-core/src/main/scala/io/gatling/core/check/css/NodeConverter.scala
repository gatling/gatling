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

package io.gatling.core.check.css

import scala.annotation.implicitNotFound

import jodd.lagarto.dom.Node

trait LowPriorityNodeConverterImplicits {

  implicit val stringNodeConverter: NodeConverter[String] = (node, nodeAttribute) =>
    nodeAttribute match {
      case Some(attr) => Option(node.getAttribute(attr))
      case _          => Some(node.getTextContent.trim)
    }

  implicit val nodeNodeConverter: NodeConverter[Node] = (node, _) => Some(node)

  implicit val formNodeConverter: NodeConverter[Map[String, Any]] = (node, _) =>
    node.getNodeName match {
      case "form" => Some(FormExtractor.extractFormInputs(node))
      case _      => None
    }
}

object NodeConverter extends LowPriorityNodeConverterImplicits {
  def apply[X: NodeConverter]: NodeConverter[X] = implicitly[NodeConverter[X]]
}

@implicitNotFound("No member of type class NodeConverter found for type ${X}")
trait NodeConverter[X] {
  def convert(node: Node, nodeAttribute: Option[String]): Option[X]
}
