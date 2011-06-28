package com.excilys.ebi.gatling.core.action.request

abstract class AbstractRequest(val name: String) {
  def getName = name
}