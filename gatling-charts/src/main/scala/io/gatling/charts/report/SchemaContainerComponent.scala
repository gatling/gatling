/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.charts.report

import io.gatling.charts.component.Component

final class SchemaContainerComponent(left: Component, right: Component) extends Component {
  override def html: String =
    s"""
       |<div class="schema-container">
       |${left.html}
       |${right.html}
       |</div>
       |""".stripMargin

  override def js: String = left.js + right.js

  override def jsFiles: Seq[String] = left.jsFiles ++ right.jsFiles
}
