package com.excilys.ebi.gatling.http.processor

import com.excilys.ebi.gatling.core.processor.Processor

import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.phase.HttpPhase

class HttpProcessor(val httpPhase: HttpPhase) extends Processor {
  def getHttpPhase = httpPhase
}