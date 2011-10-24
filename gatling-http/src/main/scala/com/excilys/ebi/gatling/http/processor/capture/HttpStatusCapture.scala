package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.provider.HttpStatusProviderType
import com.excilys.ebi.gatling.core.util.StringHelper._

class HttpStatusCapture(attrKey: String)
		extends HttpCapture((c: Context) => EMPTY, attrKey, StatusReceived, HttpStatusProviderType) {
}