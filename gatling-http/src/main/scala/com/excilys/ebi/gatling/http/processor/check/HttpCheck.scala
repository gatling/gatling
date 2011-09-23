package com.excilys.ebi.gatling.http.processor.check

import com.excilys.ebi.gatling.core.processor.CheckType._

import com.excilys.ebi.gatling.http.processor.HttpProcessor

trait HttpCheck extends HttpProcessor {
  def getCheckType: CheckType
  def getExpected: Any
}