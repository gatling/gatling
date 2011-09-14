package com.excilys.ebi.gatling.core.processor

object AssertionType extends Enumeration {
  type AssertionType = Value
  val EQUALITY, IN_RANGE = Value
}