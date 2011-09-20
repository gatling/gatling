package com.excilys.ebi.gatling.core.feeder

import com.excilys.ebi.gatling.core.util.FileHelper._

class SSVFeeder(filePath: String, mappings: List[String]) extends SeparatedValuesFeeder(filePath, mappings, SEMICOLON_SEPARATOR, SSV_EXTENSION)