package com.excilys.ebi.gatling.core.processor

object Check {
  def checkEquals(what: Any, expected: Any): Boolean = {
    what == expected;
  }

  def checkInRange(what: String, range: String): Boolean = {
    range.contains(what)
  }

  def checkInRange(what: Any, range: Any): Boolean = {
    checkInRange(what.toString, range.toString)
  }

  def checkInRange(what: Int, range: Range): Boolean = {
    range.contains(what)
  }

  def checkInRange(what: Long, range: Range): Boolean = {
    checkInRange(what.toInt, range)
  }
}