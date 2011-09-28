package com.excilys.ebi.gatling.core.processor.builtin
import com.excilys.ebi.gatling.core.processor.CheckType

object ExistenceCheckType extends CheckType {
  def doCheck(value: Option[String], range: String) = value.isDefined
}