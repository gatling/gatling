package com.excilys.ebi.gatling.http.processor.capture

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.provider.HttpStatusProviderType
import org.apache.commons.lang3.StringUtils

class HttpStatusCapture(attrKey: String)
		extends HttpCapture((c: Context) => StringUtils.EMPTY, attrKey, StatusReceived, HttpStatusProviderType) {
}