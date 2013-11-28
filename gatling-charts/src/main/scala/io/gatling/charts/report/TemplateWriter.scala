/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.charts.report

import scala.io.Codec.string2codec
import scala.tools.nsc.io.{ File, Path }

import com.dongxiguo.fastring.Fastring

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.IOHelper.withCloseable

class TemplateWriter(path: Path) {

	def writeToFile(output: Fastring) {
		withCloseable(File(path)(configuration.core.codec).writer) { output.appendTo }
	}
}
