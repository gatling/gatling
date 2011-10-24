package com.excilys.ebi.gatling.statistics.writer

import java.io.File
import java.io.FileWriter
import com.excilys.ebi.gatling.core.util.PathHelper._
import com.excilys.ebi.gatling.core.util.StringHelper._

class SeparatedValueFileWriter(val runOn: String, val fileName: String, val separator: String) {
	def writeToFile(values: List[List[String]]) = {
		val dir = new File(GATLING_RESULTS_FOLDER + "/" + runOn + GATLING_RAWDATA_FOLDER)
		dir.mkdir
		val file = new File(dir, fileName)
		val fw = new FileWriter(file, true)
		for (value <- values) {
			fw.write(value.mkString(EMPTY, separator, "\n"))
		}
		fw.close
	}
}