package com.excilys.ebi.gatling.statistics.writer

import com.excilys.ebi.gatling.core.util.FileHelper._

class TSVFileWriter(runOn: String, fileName: String) extends SeparatedValueFileWriter(runOn, fileName, TABULATION_SEPARATOR)