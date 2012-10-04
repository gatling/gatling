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

import scala.tools.nsc.io.Directory
import scala.tools.nsc.io.Path.jfile2path

import com.excilys.ebi.gatling.core.util.StringHelper.stripAccents

/**
 * This object groups all utilities for files
 */
object FileHelper {

	val COMMA_SEPARATOR = ","
	val SEMICOLON_SEPARATOR = ";"
	val TABULATION_SEPARATOR = "\t"
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
		stripAccents(s.replace("-", "_")
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
			.replace("{", "_")
			.replace("}", "_")
			.replace("[", "_")
			.replace("]", "_")
			.toLowerCase)
	}

	def requestFileName(s: String) = "req_" + formatToFilename(s) + HTML_EXTENSION

	/**
	 * Create a new temporary directory, which will be deleted upon the exit of the VM.
	 *
	 * @returns File representing the directory
	 */
	def createTempDirectory(deleteAtExit: Boolean = true): Directory = {
		val file = File.createTempFile("temp", "dir")
		file.delete

		val directory: Directory = file.createDirectory(false, false)

		if (deleteAtExit)
			Runtime.getRuntime.addShutdownHook(new Thread {
				override def run {
					directory.deleteRecursively
				}
			})

		directory
	}
}