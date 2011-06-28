package com.excilys.ebi.gatling.http.processor.capture.builder

import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider
import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.context.RequestScope
import com.excilys.ebi.gatling.http.context.SessionScope
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.phase.HttpResponseHook

abstract class TRUE
abstract class FALSE

object HttpCaptureBuilder {
  abstract class HttpCaptureBuilder[HE](val expression: Option[String], val attribute: Option[String], val scope: Option[HttpScope], val httpHook: Option[HttpResponseHook])
    extends Logging {

    def in(attrKey: String) = inRequest(attrKey)
    def inRequest(attrKey: String)
    def inSession(attrKey: String)
  }
}