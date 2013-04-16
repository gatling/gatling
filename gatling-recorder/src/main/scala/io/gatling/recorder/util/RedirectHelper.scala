package io.gatling.recorder.util

import io.gatling.recorder.config.RecorderConfiguration.configuration

object RedirectHelper {

	def isRedirectCode(code: Int) = code >= 300 && code <= 399

	def isRequestRedirectChainStart(lastStatus: Int, currentStatus: Int): Boolean = configuration.http.followRedirect && !isRedirectCode(lastStatus) && isRedirectCode(currentStatus)

	def isRequestInsideRedirectChain(lastStatus: Int, currentStatus: Int): Boolean = configuration.http.followRedirect && isRedirectCode(lastStatus) && isRedirectCode(currentStatus)

	def isRequestRedirectChainEnd(lastStatus: Int, currentStatus: Int): Boolean = configuration.http.followRedirect && isRedirectCode(lastStatus) && !isRedirectCode(currentStatus)
}
