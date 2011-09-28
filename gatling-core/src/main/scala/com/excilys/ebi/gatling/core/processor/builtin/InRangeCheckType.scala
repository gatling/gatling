package com.excilys.ebi.gatling.core.processor.builtin
import com.excilys.ebi.gatling.core.processor.CheckType

object InRangeCheckType extends CheckType {
  def doCheck(value: Option[String], range: String) = !value.isEmpty && range.contains(value.get)
}