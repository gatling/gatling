package com.excilys.ebi.gatling.http.capture

import com.excilys.ebi.gatling.core.provider.capture.RegExpCaptureProvider

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.phase.HttpResponseHook
import scala.util.matching.Regex

class HttpRegExpCapture(expression: String, attrKey: String, scope: HttpScope, httpHook: HttpResponseHook)
  extends HttpCapture(expression, attrKey, scope, httpHook, new RegExpCaptureProvider)