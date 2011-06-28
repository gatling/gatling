package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.phase.HttpPhase

class HttpXPathCapture(expression: String, attrKey: String, scope: HttpScope, httpPhase: HttpPhase)
  extends HttpCapture(expression, attrKey, scope, httpPhase, new XPathCaptureProvider)