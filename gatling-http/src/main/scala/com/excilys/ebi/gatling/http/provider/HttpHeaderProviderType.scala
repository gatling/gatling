package com.excilys.ebi.gatling.http.provider

import com.excilys.ebi.gatling.core.provider.ProviderType
import com.excilys.ebi.gatling.http.provider.capture.HttpHeadersCaptureProvider
import com.ning.http.client.FluentCaseInsensitiveStringsMap

object HttpHeaderProviderType extends ProviderType {
	def getProvider(headersMap: Any) = {
		logger.debug("Instantiation of HttpHeadersCaptureProvider")
		new HttpHeadersCaptureProvider(headersMap.asInstanceOf[FluentCaseInsensitiveStringsMap])
	}
}