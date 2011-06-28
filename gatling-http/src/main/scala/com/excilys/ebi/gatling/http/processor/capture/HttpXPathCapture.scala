package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider

import com.excilys.ebi.gatling.http.context.HttpScope
import com.excilys.ebi.gatling.http.phase.HttpResponseHook

class HttpXPathCapture(expression: String, attrKey: String, scope: HttpScope, httpHook: HttpResponseHook)
  extends HttpCapture(expression, attrKey, scope, httpHook, new XPathCaptureProvider)