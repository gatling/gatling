package com.excilys.ebi.gatling.statistics.writer

class TSVFileWriter(runOn: String, fileName: String) extends SeparatedValueFileWriter(runOn, fileName, "\t")