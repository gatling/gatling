package com.excilys.ebi.gatling.statistics.writer

class SSVFileWriter(runOn: String, fileName: String) extends SeparatedValueFileWriter(runOn, fileName, ";")