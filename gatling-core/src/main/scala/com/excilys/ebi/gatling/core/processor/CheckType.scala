package com.excilys.ebi.gatling.core.processor

object CheckType extends Enumeration {
  type CheckType = Value
  val EQUALITY, INEQUALITY, IN_RANGE, EXISTENCE, INEXISTENCE = Value
}