package com.excilys.ebi.gatling.http.capture.builder

import com.excilys.ebi.gatling.core.capture.provider.XPathCaptureProvider

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.context.RequestScope
import com.excilys.ebi.gatling.http.context.SessionScope
import com.excilys.ebi.gatling.http.capture.HttpCapture
import com.excilys.ebi.gatling.http.phase.HttpResponseHook

abstract class TRUE
abstract class FALSE

object HttpCaptureBuilder {
  abstract class HttpCaptureBuilder[HE](val expression: Option[String], val attribute: Option[String], val scope: Option[HttpScope], val httpHook: Option[HttpResponseHook]) {

    def in(attrKey: String) = inRequest(attrKey)
    def inRequest(attrKey: String)
    def inSession(attrKey: String)
  }
}