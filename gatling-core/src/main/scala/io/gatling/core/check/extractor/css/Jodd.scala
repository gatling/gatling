/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import scala.collection.mutable

import jodd.lagarto.LagartoParser
import jodd.lagarto.dom.{ Node, LagartoDomBuilderConfig, LagartoDOMBuilder }
import jodd.log.LoggerFactory
import jodd.log.impl.Slf4jLoggerFactory

object Jodd {

  LoggerFactory.setLoggerFactory(new Slf4jLoggerFactory)

  val IeVersionDroppingCc = 10.0

  private def joddConfigBase =
    new LagartoDomBuilderConfig()
      .setParsingErrorLogLevelName("INFO")
      .setCaseSensitive(false)

  val JoddConfig = joddConfigBase
    .setEnableConditionalComments(false)

  def getJoddConfig(ieVersion: Option[Float]): LagartoDomBuilderConfig =
    ieVersion match {
      case Some(version) if version < IeVersionDroppingCc =>
        joddConfigBase
          .setEnableConditionalComments(true)
          .setCondCommentIEVersion(version)

      case _ => JoddConfig
    }

  def newLagartoDomBuilder: LagartoDOMBuilder = {
    val domBuilder = new LagartoDOMBuilder
    domBuilder.setConfig(JoddConfig)
    domBuilder
  }

  def newLagartoParser(string: String, ieVersion: Option[Float]): LagartoParser = {
    val lagartoParser = new LagartoParser(string, false)
    lagartoParser.setConfig(getJoddConfig(ieVersion))
    lagartoParser
  }

  def extractFormInputs(node: Node): Map[String, Seq[String]] = {

      def extractInput(node: Node, parameters: mutable.MultiMap[String, String]): Unit =
        for {
          typeAttr <- Option(node.getAttribute("type"))
          nameAttr <- Option(node.getAttribute("name"))
          valueAttr <- Option(node.getAttribute("value")).orElse(if (typeAttr == "checkbox" && node.hasAttribute("checked")) Some("on") else None)
        } parameters.addBinding(nameAttr, valueAttr)

      def extractSelect(node: Node, parameters: mutable.MultiMap[String, String]): Unit =
        Option(node.getAttribute("name")).foreach(extractOptions(_, node, parameters))

      def extractOptions(selectNameAttr: String, node: Node, parameters: mutable.MultiMap[String, String]): Unit =
        for (i <- 0 until node.getChildNodesCount) {
          val child = node.getChild(i)
          child.getNodeName match {
            case "option" =>
              for {
                valueAttr <- Option(child.getAttribute("value")) if child.hasAttribute("selected")
              } parameters.addBinding(selectNameAttr, valueAttr)

            case _ =>
              extractOptions(selectNameAttr, child, parameters)
          }
        }

      def extractTextArea(node: Node, parameters: mutable.MultiMap[String, String]): Unit =
        for {
          nameAttr <- Option(node.getAttribute("name"))
          valueAttr <- Option(node.getTextContent) if valueAttr.nonEmpty
        } parameters.addBinding(nameAttr, valueAttr)

      def processForm(node: Node, parameters: mutable.MultiMap[String, String]): Unit =
        for (i <- 0 until node.getChildNodesCount) {
          val currentNode = node.getChild(i)
          currentNode.getNodeName match {
            case "input"    => extractInput(currentNode, parameters)
            case "select"   => extractSelect(currentNode, parameters)
            case "textarea" => extractTextArea(currentNode, parameters)
            case _          => processForm(currentNode, parameters)
          }
        }

    val parameters = new mutable.HashMap[String, mutable.Set[String]] with mutable.MultiMap[String, String]
    processForm(node, parameters)
    parameters.toMap.map { case (k, v) => k -> v.toSeq }
  }
}
