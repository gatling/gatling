package com.excilys.ebi.gatling.core.feeder

class TSVFeeder(filePath: String, mappings: List[String]) extends SeparatedValuesFeeder(filePath, mappings, "\t", ".tsv")