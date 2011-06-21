package com.excilys.ebi.gatling.http.processor

import com.excilys.ebi.gatling.core.processor.Processor

import com.excilys.ebi.gatling.http.phase.HttpResponseHook
import com.excilys.ebi.gatling.http.context.HttpScope

class HttpProcessor(val httpHook: HttpResponseHook) extends Processor {
  def getHttpHook = httpHook
}