package com.excilys.ebi.gatling.core.feeder

class SSVFeeder(filePath: String, mappings: List[String]) extends SeparatedValuesFeeder(filePath, mappings, ";", ".ssv")