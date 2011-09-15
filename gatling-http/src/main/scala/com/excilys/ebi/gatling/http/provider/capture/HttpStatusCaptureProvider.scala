package com.excilys.ebi.gatling.http.provider.capture

import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider
import com.ning.http.client.Response
import com.excilys.ebi.gatling.core.log.Logging

object HttpStatusCaptureProvider extends Logging {

  var captureProviderPool: Map[String, HttpStatusCaptureProvider] = Map.empty

  def prepare(identifier: String, statusCode: Int) = {
    captureProviderPool += (identifier -> new HttpStatusCaptureProvider(statusCode))
  }

  def capture(identifier: String, statusCode: Any) = {
    captureProviderPool.get(identifier).get.capture(statusCode)
  }

  def clear(identifier: String) = {
    captureProviderPool -= identifier
  }
}
class HttpStatusCaptureProvider(statusCode: Int) extends AbstractCaptureProvider {
  def capture(unused: Any): Option[String] = {
    Some(statusCode.toString)
  }
}