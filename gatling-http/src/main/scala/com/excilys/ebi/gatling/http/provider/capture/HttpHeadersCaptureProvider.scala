package com.excilys.ebi.gatling.http.provider.capture

import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider

import com.excilys.ebi.gatling.http.header.HeaderKey

import com.ning.http.client.FluentCaseInsensitiveStringsMap

class HttpHeadersCaptureProvider extends AbstractCaptureProvider {

  def captureAll(target: Any, from: Any): Option[java.util.List[String]] = {
    val headersMap = from match {
      case map: FluentCaseInsensitiveStringsMap => map
      case _ => throw new IllegalArgumentException
    }

    val headerKey = target match {
      case key: HeaderKey => key.toString
      case s: String => s
      case _ => throw new IllegalArgumentException
    }

    val values = headersMap.get(headerKey)

    if (values == null)
      None
    else
      Option(values)
  }

  def captureOne(target: Any, from: Any): Option[Any] = {
    captureAll(target, from).map { list =>
      if (list.size > 0)
        Some(list.get(0))
      else
        None
    }.getOrElse(None)
  }
}