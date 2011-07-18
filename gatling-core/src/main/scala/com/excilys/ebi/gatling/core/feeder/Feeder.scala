package com.excilys.ebi.gatling.core.feeder

import com.excilys.ebi.gatling.core.log.Logging

abstract class Feeder(val filePath: String, val mappings: List[String]) extends Logging {
  def get(key: String)(index: Int): String
  def nextIndex: Int
}