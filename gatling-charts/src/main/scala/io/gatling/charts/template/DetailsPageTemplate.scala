/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.charts.template

import java.{ lang => jl }

import io.gatling.charts.component.Component
import io.gatling.charts.report.{ Container, GroupContainer, RequestContainer }
import io.gatling.charts.stats.RunInfo
import io.gatling.charts.util.HtmlHelper.HtmlRichString

private[charts] final class DetailsPageTemplate(
    runInfo: RunInfo,
    title: String,
    selectedContainer: Container,
    rootContainer: GroupContainer,
    components: Component*
) extends PageTemplate(runInfo, title, rootContainer, components: _*) {
  override protected def getSubMenu: String =
    s"""<div class="item"><a href="index.html">Global</a></div>
       |<div class="item ouvert"><a id="details_link" href="$getFirstDetailPageUrl">Details</a></div>""".stripMargin

  private def isSelected(container: Container): Boolean = (container, selectedContainer) match {
    case (groupContainer: GroupContainer, selectedGroupContainer: GroupContainer) => groupContainer.group == selectedGroupContainer.group
    case (requestContainer: RequestContainer, selectedRequestContainer: RequestContainer) =>
      requestContainer.group == selectedRequestContainer.group && requestContainer.name == selectedRequestContainer.name
    case _ => false
  }

  override protected def getMenu: String = {
    def menuItem(container: Container, level: Int, parent: Option[String]): String = {
      val dataParent = parent match {
        case Some(p) => s"""data-parent="${if (level == 0) GroupContainer.RootId else s"menu-$p"}""""
        case _       => ""
      }

      val expandButtonStyle = container match {
        case _: GroupContainer => ""
        case _                 => "hidden"
      }

      val liClass = if (isSelected(container)) """class="on"""" else ""

      s"""<li $dataParent $liClass>
         |  <span id="menu-${container.id}" style="margin-left: ${level * 10}px;" class="expand-button $expandButtonStyle">&nbsp;</span>
         |  <a href="${container.id}.html" class="item withTooltip">
         |    <span class="nav-tooltip" id="parent-menu-label-${container.id}" data-toggle="popover" data-placement="right" data-container="body" data-content="">
         |      <span onmouseover="isEllipsed('menu-label-${container.id}')" id="menu-label-${container.id}" class="nav-label ellipsed-name">${container.name.htmlEscape}</span>
         |    </span>
         |  </a>
         |</li>""".stripMargin
    }

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def menuItemsForGroup(group: GroupContainer, level: Int, parent: Option[String]): String = {
      val buffer = new jl.StringBuilder

      if (level > 0) {
        buffer.append(menuItem(group, level - 1, parent))
      }

      val newParent = Some(group.id)

      group.groups.values.foreach { subGroup =>
        buffer.append(menuItemsForGroup(subGroup, level + 1, newParent))
      }

      group.requests.values.foreach { request =>
        buffer.append(menuItem(request, level, newParent))
      }

      buffer.toString
    }

    menuItemsForGroup(rootContainer, 0, None)
  }

  override protected def onDocumentReady: String =
    """$('.nav').expandable();
      |$('.nav-tooltip').popover({trigger:'hover'});""".stripMargin
}
