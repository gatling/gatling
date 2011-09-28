package com.excilys.ebi.gatling.core.processor.builtin
import com.excilys.ebi.gatling.core.processor.CheckType

object NonEqualityCheckType extends CheckType {
  def doCheck(value: Option[String], expected: String) = !value.isEmpty && value.get != expected
}