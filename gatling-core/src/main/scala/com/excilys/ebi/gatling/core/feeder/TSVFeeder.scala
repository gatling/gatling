package com.excilys.ebi.gatling.core.feeder

import com.excilys.ebi.gatling.core.util.FileHelper._

class TSVFeeder(filePath: String, mappings: List[String]) extends SeparatedValuesFeeder(filePath, mappings, TABULATION_SEPARATOR, TSV_EXTENSION)