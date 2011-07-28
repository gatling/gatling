package com.excilys.ebi.gatling.core.feeder

class CSVFeeder(filePath: String, mappings: List[String]) extends SeparatedValuesFeeder(filePath, mappings, ",")