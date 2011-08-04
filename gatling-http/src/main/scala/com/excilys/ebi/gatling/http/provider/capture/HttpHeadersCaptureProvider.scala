package com.excilys.ebi.gatling.http.provider.capture

import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider

import com.ning.http.client.FluentCaseInsensitiveStringsMap

class HttpHeadersCaptureProvider extends AbstractCaptureProvider {

  def captureAll(target: Any, from: Any): Option[java.util.List[String]] = {

    val headersMap = from match {
      case map: FluentCaseInsensitiveStringsMap => map
      case _ => throw new IllegalArgumentException
    }

    logger.debug(" -- Headers Capture Provider - Got headers Map: {}", headersMap)

    val headerKey = target match {
      case s: String => s
      case _ => throw new IllegalArgumentException
    }

    logger.debug(" -- Headers Capture Provider - Got headerKey: {}", headerKey)

    val values = headersMap.get(headerKey)

    logger.debug(" -- Headers Capture Provider - Got header values: {}", values)

    if (values == null)
      None
    else
      Option(values)
  }

  def captureOne(target: Any, from: Any): Option[String] = {
    captureAll(target, from).map { list =>
      if (list.size > 0)
        Some(list.get(0))
      else
        None
    }.getOrElse(None)
  }
}