package com.excilys.ebi.gatling.core.processor.builtin
import com.excilys.ebi.gatling.core.processor.CheckType

object InRangeCheckType extends CheckType {
  def doCheck(value: Option[String], range: String) = {
    if (value.isEmpty)
      false
    else
      range.contains(value.get)
  }
}