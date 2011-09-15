package com.excilys.ebi.gatling.http.provider.capture

import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider

import com.ning.http.client.FluentCaseInsensitiveStringsMap

object HttpHeadersCaptureProvider {

  var captureProviderPool: Map[String, HttpHeadersCaptureProvider] = Map.empty

  def prepare(identifier: String, headersMap: FluentCaseInsensitiveStringsMap) = {
    captureProviderPool += (identifier -> new HttpHeadersCaptureProvider(headersMap))
  }

  def capture(identifier: String, expression: Any) = {
    captureProviderPool.get(identifier).get.capture(expression)
  }

  def clear(identifier: String) = {
    captureProviderPool -= identifier
  }

}
class HttpHeadersCaptureProvider(headersMap: FluentCaseInsensitiveStringsMap) extends AbstractCaptureProvider {

  def capture(headerName: Any): Option[String] = {
    captureAll(headerName.toString, headersMap).map { list =>
      if (list.size > 0)
        Some(list.get(0))
      else
        None
    }.getOrElse(None)
  }

  def captureAll(headerName: String, headersMap: FluentCaseInsensitiveStringsMap): Option[java.util.List[String]] = {

    val values = headersMap.get(headerName)

    logger.debug(" -- Headers Capture Provider - Got header values: {}", values)

    if (values == null)
      None
    else
      Some(values)
  }
}