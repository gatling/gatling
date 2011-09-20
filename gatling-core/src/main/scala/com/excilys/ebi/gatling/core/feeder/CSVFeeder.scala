package com.excilys.ebi.gatling.core.feeder

import com.excilys.ebi.gatling.core.util.FileHelper._

class CSVFeeder(filePath: String, mappings: List[String]) extends SeparatedValuesFeeder(filePath, mappings, COMMA_SEPARATOR, CSV_EXTENSION)