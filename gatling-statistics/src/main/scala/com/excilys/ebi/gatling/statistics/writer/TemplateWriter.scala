package com.excilys.ebi.gatling.statistics.writer

import java.io.File
import java.io.FileWriter

import com.excilys.ebi.gatling.core.util.PathHelper._

class TemplateWriter(val runOn: String, val fileName: String) {
	def writeToFile(output: String) = {
		val dir = new File(GATLING_RESULTS_FOLDER + "/" + runOn)
		dir.mkdir
		val file = new File(dir, fileName)
		val fw = new FileWriter(file)
		fw.write(output)
		fw.close
	}
}