package com.excilys.ebi.gatling.statistics.writer

import com.excilys.ebi.gatling.core.util.FileHelper._

class SSVFileWriter(runOn: String, fileName: String) extends SeparatedValueFileWriter(runOn, fileName, SEMICOLON_SEPARATOR)