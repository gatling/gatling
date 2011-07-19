package com.excilys.ebi.gatling.core.feeder

import com.excilys.ebi.gatling.core.log.Logging

abstract class Feeder(val filePath: String, val mappings: List[String]) extends Logging {
  def next: Map[String, String]
}