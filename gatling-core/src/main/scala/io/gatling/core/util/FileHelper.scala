/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.core.util

import io.gatling.core.util.StringHelper.RichString

/**
 * This object groups all utilities for files
 */
object FileHelper {

	val commaSeparator = ","
	val semicolonSeparator = ";"
	val tabulationSeparator = "\t"

	implicit class FileRichString(val string: String) extends AnyVal {

		/**
		 * Transform a string to a simpler one that can be used safely as file name
		 *
		 * @param s the string to be simplified
		 * @return a simplified string
		 */
		def toFilename = string
			.trim
			.replace("-", "_")
			.replace(" ", "_")
			.replace("'", "_")
			.replace('/', '_')
			.replace(':', '_')
			.replace('?', '_')
			.replace('"', '_')
			.replace('<', '_')
			.replace('>', '_')
			.replace('|', '_')
			.replace("{", "_")
			.replace("}", "_")
			.replace("[", "_")
			.replace("]", "_")
			.replace("(", "_")
			.replace(")", "_")
			.replace(".", "_")
			.toLowerCase
			.stripAccents match {
				case "" => "missing_name"
				case s => s
			}
		
		def toRequestFileName = s"req_${string.toFilename}.html"
	}
}