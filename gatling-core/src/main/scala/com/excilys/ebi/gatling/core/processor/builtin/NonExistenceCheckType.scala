package com.excilys.ebi.gatling.core.processor.builtin
import com.excilys.ebi.gatling.core.processor.CheckType

object NonExistenceCheckType extends CheckType {
  def doCheck(value: Option[String], range: String) = {
    value.isEmpty
  }
}