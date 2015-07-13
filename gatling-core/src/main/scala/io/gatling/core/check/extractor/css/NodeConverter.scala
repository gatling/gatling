/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.check.extractor.css

import jodd.lagarto.dom.Node

import scala.annotation.implicitNotFound
import scala.collection.mutable

trait LowPriorityNodeConverterImplicits {

  implicit val stringNodeConverter = new NodeConverter[String] {
    def convert(node: Node, nodeAttribute: Option[String]): Option[String] = nodeAttribute match {
      case Some(attr) => Option(node.getAttribute(attr))
      case _          => Some(node.getTextContent.trim)
    }
  }

  implicit val nodeNodeConverter = new NodeConverter[Node] {
    def convert(node: Node, nodeAttribute: Option[String]): Option[Node] = Some(node)
  }

  private def processInput(node: Node, parameters: mutable.MultiMap[String, String]): Unit = {
    val typeAttr = node.getAttribute("type")

    val isCheckbox = typeAttr.equals("checkbox")
    val isRadio = typeAttr.equals("radio")

    if (isRadio || isCheckbox) {
      if (!node.hasAttribute("checked")) return
    }

    val nameAttr = node.getAttribute("name")
    if (nameAttr == null) return

    var valueAttr = node.getAttribute("value")
    if (valueAttr == null && isCheckbox) valueAttr = "on"

    // FIXME if valueAttr null?
    parameters.addBinding(nameAttr, valueAttr)
  }

  private def processSelect(originNameAttr: String, node: Node, parameters: mutable.MultiMap[String, String]): Unit = {
    node.getNodeName match {
      case "option" =>
        if (node.hasAttribute("selected")) {
          val valueAttr = node.getAttribute("value")
          // FIXME if valueAttr null?
          parameters.addBinding(originNameAttr, valueAttr)
        }
      case _ =>
        val nodesCount = node.getChildNodesCount
        for (i <- 0 until nodesCount) processSelect(originNameAttr, node.getChild(i), parameters)
    }
  }

  private def processTextArea(node: Node, parameters: mutable.MultiMap[String, String]): Unit = {
    val nameAttr = node.getAttribute("name")
    val valueAttr = node.getTextContent
    // FIXME if valueAttr null?
    parameters.addBinding(nameAttr, valueAttr)
  }

  private def processForm(node: Node, parameters: mutable.MultiMap[String, String]): Unit = {
    val nodesCount = node.getChildNodesCount
    for (i <- 0 until nodesCount) {
      val currentNode = node.getChild(i)
      currentNode.getNodeName match {
        case "input"    => processInput(currentNode, parameters)
        case "select"   => processSelect(currentNode.getAttribute("name"), currentNode, parameters)
        case "textarea" => processTextArea(currentNode, parameters)
        case _          => processForm(currentNode, parameters)
      }
    }
  }

  implicit val formNodeConverter = new NodeConverter[Map[String, Seq[String]]] {
    def convert(node: Node, nodeAttribute: Option[String]): Option[Map[String, Seq[String]]] = {

      val parameters = new mutable.HashMap[String, mutable.Set[String]] with mutable.MultiMap[String, String]
      processForm(node, parameters)

      Some(parameters.toMap.map { case (k, v) => k -> v.toSeq })
    }
  }
}

object NodeConverter extends LowPriorityNodeConverterImplicits {
  def apply[X: NodeConverter] = implicitly[NodeConverter[X]]
}

@implicitNotFound("No member of type class NodeConverter found for type ${X}")
trait NodeConverter[X] {
  def convert(node: Node, nodeAttribute: Option[String]): Option[X]
}
