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
package com.excilys.ebi.gatling.core.util

import java.io.File
import org.apache.commons.io.FileUtils
import com.excilys.ebi.gatling.core.util.StringHelper.stripAccents
import scala.tools.nsc.io.Directory

/**
 * This object groups all utilities for files
 */
object FileHelper {

	val COMMA_SEPARATOR = ','
	val SEMICOLON_SEPARATOR = ';'
	val TABULATION_SEPARATOR = '\t'
	val TABULATION_SEPARATOR_STRING = TABULATION_SEPARATOR.toString
	val CSV_EXTENSION = ".csv"
	val SSV_EXTENSION = ".ssv"
	val TSV_EXTENSION = ".tsv"
	val SSP_EXTENSION = ".ssp"
	val HTML_EXTENSION = ".html"

	/**
	 * Transform a string to a simpler one that can be used safely as file name
	 *
	 * @param s the string to be simplified
	 * @return a simplified string
	 */
	def formatToFilename(s: String) = {
		"req_" + stripAccents(s.replace("-", "_")
			.replace(" ", "_")
			.replace("__", "_")
			.replace("'", "")
			.replace('/', '_')
			.replace(':', '_')
			.replace('?', '_')
			.replace('"', '_')
			.replace('<', '_')
			.replace('>', '_')
			.replace('|', '_')
			.replace("__", "_")
			.toLowerCase)
	}

	/**
	 * Create a new temporary directory, which will be deleted upon the exit of the VM.
	 *
	 * @returns File representing the directory
	 */
	def createTempDirectory(deleteAtExit: Boolean = true): Directory = {
		var file = File.createTempFile("temp", "dir")
		file.delete
		file.mkdir

		if (deleteAtExit)
			Runtime.getRuntime.addShutdownHook(new Thread {
				override def run {
					FileUtils.deleteDirectory(file)
				}
			})

		Directory(file)
	}
}