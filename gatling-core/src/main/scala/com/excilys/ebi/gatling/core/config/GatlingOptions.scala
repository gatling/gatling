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
package com.excilys.ebi.gatling.core.config

import scala.tools.nsc.io.Path
import scala.tools.nsc.io.Directory

object GatlingOptions {
	val DEFAULT_RUN_ID = "run"
}

case class GatlingOptions(
	var reportsOnlyDirectoryName: Option[String] = None,
	var noReports: Boolean = false,
	var configFilePath: Option[String] = None,
	var resultsDirectory: Option[Path] = None,
	var dataDirectory: Option[Path] = None,
	var requestBodiesDirectory: Option[Path] = None,
	var simulationSourcesDirectory: Option[Directory] = None,
	var simulationBinariesDirectory: Option[Directory] = None,
	var simulationClassNames: Option[List[String]] = None,
	var runName: String = GatlingOptions.DEFAULT_RUN_ID)