/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.charts.template

import com.excilys.ebi.gatling.charts.config.ChartsFiles.GATLING_TEMPLATE_MENU_JS_FILE
import com.excilys.ebi.gatling.core.util.PathHelper._
import scala.collection.mutable.MutableList

class MenuTemplate(requestLinks: Iterable[(String, Option[String], String)]) {

	def getOutput: String = PageTemplate.TEMPLATE_ENGINE.layout(GATLING_TEMPLATE_MENU_JS_FILE, Map("requestLinks" -> requestLinks))
}