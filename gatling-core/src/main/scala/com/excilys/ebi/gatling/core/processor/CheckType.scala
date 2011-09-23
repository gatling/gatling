package com.excilys.ebi.gatling.core.processor

object CheckType extends Enumeration {
  type CheckType = Value
  val EQUALITY, IN_RANGE, EXISTENCE = Value
}