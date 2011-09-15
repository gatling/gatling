package com.excilys.ebi.gatling.http.provider.capture

import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider

class HttpStatusCaptureProvider(statusCode: Int) extends AbstractCaptureProvider {
  def capture(unused: Any): Option[String] = {
    Some(statusCode.toString)
  }
}