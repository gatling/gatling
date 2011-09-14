package com.excilys.ebi.gatling.core.processor

object Assertion {
  def assertEquals(what: Any, expected: Any): Boolean = {
    what == expected;
  }

  def assertInRange(what: String, range: String): Boolean = {
    range.contains(what)
  }

  def assertInRange(what: Any, range: Any): Boolean = {
    assertInRange(what.toString, range.toString)
  }

  def assertInRange(what: Int, range: Range): Boolean = {
    range.contains(what)
  }

  def assertInRange(what: Long, range: Range): Boolean = {
    assertInRange(what.toInt, range)
  }
}