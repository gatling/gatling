package com.excilys.ebi.gatling.statistics.writer

class CSVFileWriter(runOn: String, fileName: String) extends SeparatedValueFileWriter(runOn, fileName, ",")