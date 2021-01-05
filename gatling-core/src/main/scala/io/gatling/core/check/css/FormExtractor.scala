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

import java.util.Locale

import jodd.lagarto.dom.Node

private[css] object FormExtractor {

  private sealed trait Input { def name: String }
  private sealed trait SelectInput extends Input
  private sealed trait SingleValueInput extends Input { def value: String }
  private final case class SingleSelectInput(name: String, value: String) extends SelectInput with SingleValueInput
  private final case class MultipleSelectInput(name: String, values: Seq[String]) extends SelectInput
  private final case class RadioInput(name: String, value: String) extends SingleValueInput
  private final case class CheckboxInput(name: String, value: String, checked: Boolean) extends SingleValueInput
  private final case class RegularInput(name: String, value: String) extends SingleValueInput

  private val IgnoredInputTypes = Set("submit", "reset", "button", "file")

  private def extractInput(node: Node): Option[SingleValueInput] =
    for {
      name <- Option(node.getAttribute("name"))
      if !name.isEmpty && !node.hasAttribute("disabled")
      typeAttr <- Option(node.getAttribute("type")).map(_.toLowerCase(Locale.ROOT))
      checked = node.hasAttribute("checked")
      if !IgnoredInputTypes.contains(
        typeAttr
      ) && (typeAttr != "radio" || checked) // discard unchecked radios, but we want the information that a checkbox was multiple
      value <-
        if (typeAttr == "checkbox") {
          Option(node.getAttribute("value")).orElse(Some("on"))
        } else {
          Option(node.getAttribute("value"))
        }
    } yield typeAttr match {
      case "radio"    => RadioInput(name, value)
      case "checkbox" => CheckboxInput(name, value, checked)
      case _          => RegularInput(name, value)
    }

  private final case class SelectOption(value: String, selected: Boolean)

  private def extractSelect(node: Node): Option[SelectInput] = {

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def extractOptions(currentNode: Node, values: List[SelectOption]): List[SelectOption] =
      (for {
        i <- 0 until currentNode.getChildNodesCount
        child = currentNode.getChild(i)
        option <- child.getNodeName match {
          case "option" =>
            Option(child.getAttribute("value")) match {
              case Some(value) => SelectOption(value, child.hasAttribute("selected")) :: values
              case _           => values
            }
          case _ =>
            extractOptions(child, values)
        }
      } yield option).toList

    for {
      name <- Option(node.getAttribute("name"))
      if !name.isEmpty && !node.hasAttribute("disabled")
      options = extractOptions(node, Nil)
      if options.nonEmpty
      selectedOptionValues = options.collect { case SelectOption(value, true) => value }
      isMultiple = node.hasAttribute("multiple")
      if !isMultiple || selectedOptionValues.nonEmpty
    } yield {
      if (isMultiple) {
        MultipleSelectInput(name, selectedOptionValues)
      } else if (selectedOptionValues.isEmpty) {
        // no selected options, the first option is selected by default
        SingleSelectInput(name, options.head.value)
      } else {
        SingleSelectInput(name, selectedOptionValues.head)
      }
    }
  }

  private def extractTextArea(node: Node): Option[SingleValueInput] =
    for {
      name <- Option(node.getAttribute("name"))
      if !name.isEmpty && !node.hasAttribute("disabled")
    } yield RegularInput(name, Option(node.getTextContent).getOrElse(""))

  private def processForm(formNode: Node): Seq[Input] = {
    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def processFormRec(currentNode: Node, inputs: Seq[Input]): Seq[Input] = {
      val childInputs =
        for {
          i <- 0 until currentNode.getChildNodesCount
          childNode = currentNode.getChild(i)
          child <- childNode.getNodeName match {
            case "input"    => extractInput(childNode).toList
            case "select"   => extractSelect(childNode).toList
            case "textarea" => extractTextArea(childNode).toList
            case _          => processFormRec(childNode, inputs)
          }
        } yield child

      inputs ++ childInputs
    }

    processFormRec(formNode, Nil)
  }

  private def filterNonCheckedCheckboxes(groupedInputs: Map[String, Seq[SingleValueInput]]): Map[String, Seq[SingleValueInput]] =
    groupedInputs.view
      .mapValues { inputs =>
        inputs.filter {
          case CheckboxInput(_, _, checked) => checked
          case _                            => true
        }
      }
      .to(Map)

  def extractFormInputs(node: Node): Map[String, Any] = {

    val allInputs = processForm(node)

    val nonEmptyMultipleSelectInputs: Map[String, Seq[String]] = allInputs.collect {
      case MultipleSelectInput(name, values) if values.nonEmpty => name -> values
    }.toMap

    val (multiValuedInputs, singleValuedInputs) =
      allInputs.collect { case single: SingleValueInput => single }.groupBy(_.name).partition { case (_, inputs) => inputs.size > 1 }

    val nonEmptyMultiValuedInputs: Map[String, Seq[String]] = filterNonCheckedCheckboxes(multiValuedInputs).collect {
      case (key, nonEmptyInputs) if nonEmptyInputs.nonEmpty => key -> nonEmptyInputs.map(_.value)
    }

    val nonEmptySingleValuedInputs: Map[String, String] = filterNonCheckedCheckboxes(singleValuedInputs).collect {
      case (key, nonEmptyInputs) if nonEmptyInputs.nonEmpty => key -> nonEmptyInputs.head.value
    }

    nonEmptyMultipleSelectInputs ++ nonEmptyMultiValuedInputs ++ nonEmptySingleValuedInputs
  }
}
