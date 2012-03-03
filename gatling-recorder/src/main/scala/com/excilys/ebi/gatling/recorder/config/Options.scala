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
package com.excilys.ebi.gatling.recorder.config

class Options(
	var localPort: Option[Int] = None,
	var localPortSsl: Option[Int] = None,
	var proxyHost: Option[String] = None,
	var proxyPort: Option[Int] = None,
	var proxyPortSsl: Option[Int] = None,
	var outputFolder: Option[String] = None,
	var requestBodiesFolder: Option[String] = None,
	var simulationClassName: Option[String] = None,
	var simulationPackage: Option[String] = None,
	var encoding: Option[String] = None,
	var followRedirect: Option[Boolean] = None)