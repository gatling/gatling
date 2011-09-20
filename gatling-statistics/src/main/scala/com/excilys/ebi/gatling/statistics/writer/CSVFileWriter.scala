package com.excilys.ebi.gatling.statistics.writer

import com.excilys.ebi.gatling.core.util.FileHelper._

class CSVFileWriter(runOn: String, fileName: String) extends SeparatedValueFileWriter(runOn, fileName, COMMA_SEPARATOR)