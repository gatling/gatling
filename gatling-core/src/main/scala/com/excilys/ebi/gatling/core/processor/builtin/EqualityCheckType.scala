package com.excilys.ebi.gatling.core.processor.builtin
import com.excilys.ebi.gatling.core.processor.CheckType

object EqualityCheckType extends CheckType {
  def doCheck(value: Option[String], expected: String) = {
    if (value.isEmpty)
      false
    else
      value.get == expected
  }
}