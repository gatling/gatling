/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.charts.template
import scala.collection.immutable.SortedSet
import org.fusesource.scalate.TemplateEngine
import com.excilys.ebi.gatling.charts.component.Component
import com.excilys.ebi.gatling.charts.util.PathHelper.GATLING_TEMPLATE_LAYOUT_FILE
import com.excilys.ebi.gatling.core.log.Logging

object PageTemplate {
	val engine = new TemplateEngine
	engine.allowReload = false
	engine.escapeMarkup = false
}

abstract class PageTemplate(title: String, subTitle: String, components: Component*) extends Logging {

	val jsFiles = (Seq("jquery.min.js", "menu.js") ++ getAdditionnalJSFiles).distinct

	def getContent: String = (for (component <- components) yield component.getHTMLContent).mkString

	def getJavascript: String = (for (component <- components) yield component.getJavascriptContent).mkString

	def getAdditionnalJSFiles: Seq[String] = {
		var seq: Seq[String] = Seq.empty
		for (component <- components) {
			seq ++= component.getJavascriptFiles
		}
		seq
	}

	def getOutput: String = {
		PageTemplate.engine.layout(GATLING_TEMPLATE_LAYOUT_FILE,
			Map("jsFiles" -> jsFiles,
				"pageTitle" -> title,
				"pageSubTitle" -> subTitle,
				"pageContent" -> getContent,
				"javascript" -> getJavascript))
	}
}