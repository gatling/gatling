package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.provider.HttpXPathProviderType

class HttpXPathCapture(expression: Context => String, attrKey: String, httpPhase: HttpPhase)
		extends HttpCapture(expression, attrKey, httpPhase, HttpXPathProviderType) {
}