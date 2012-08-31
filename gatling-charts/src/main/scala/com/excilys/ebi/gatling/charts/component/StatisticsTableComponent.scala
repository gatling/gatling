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
package com.excilys.ebi.gatling.charts.component

import com.excilys.ebi.gatling.charts.config.ChartsFiles.{ GATLING_TEMPLATE_STATISTICS_TABLE_HTML_TEMPLATE_URL, GATLING_TEMPLATE_STATISTICS_TABLE_JS_TEMPLATE_URL }
import com.excilys.ebi.gatling.charts.template.PageTemplate.TEMPLATE_ENGINE

class StatisticsTableComponent extends Component {

	val getHTMLContent = TEMPLATE_ENGINE.layout(GATLING_TEMPLATE_STATISTICS_TABLE_HTML_TEMPLATE_URL)

	val getJavascriptContent = TEMPLATE_ENGINE.layout(GATLING_TEMPLATE_STATISTICS_TABLE_JS_TEMPLATE_URL)

	val getJavascriptFiles = Nil
}
