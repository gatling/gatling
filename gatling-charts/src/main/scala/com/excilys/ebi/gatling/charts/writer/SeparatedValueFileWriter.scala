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
package com.excilys.ebi.gatling.charts.writer
import java.io.FileWriter

import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.{ File, Directory }

import com.excilys.ebi.gatling.core.config.GatlingFiles.rawdataFolder
import com.excilys.ebi.gatling.core.util.PathHelper.path2jfile
import com.excilys.ebi.gatling.core.util.Resource.use
import com.excilys.ebi.gatling.core.util.StringHelper.{ END_OF_LINE, EMPTY }

class SeparatedValueFileWriter(val runOn: String, val fileName: String, val separator: Char) {
	def writeToFile(values: List[List[String]]) = {
		Directory(rawdataFolder(runOn)).createDirectory()
		val stringSeparator = separator.toString

		use(new FileWriter(File(rawdataFolder(runOn) / fileName), true)) { fw =>
			for (value <- values) {
				fw.write(value.mkString(EMPTY, stringSeparator, END_OF_LINE))
			}
		}
	}
}