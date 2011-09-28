package com.excilys.ebi.gatling.core.processor

trait CheckType {
  def doCheck(value: Option[String], expected: String): Boolean
}