package com.excilys.ebi.gatling.http.processor.check

import com.excilys.ebi.gatling.core.processor.CheckType

import com.excilys.ebi.gatling.http.processor.HttpProcessor

trait HttpCheck extends HttpProcessor {
  def getCheckType: CheckType
  def getExpected: String

  def getResult(value: Option[String]): Boolean = {
    getCheckType.doCheck(value, getExpected)
  }
}