package com.excilys.ebi.gatling.http.provider.capture

import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider

import com.ning.http.client.Response

class HttpStatusCaptureProvider extends AbstractCaptureProvider {
  def captureOne(target: Any, from: Any): Option[Any] = {
    from match {
      case i: Int => Some(i)
      case _ => throw new IllegalArgumentException
    }
  }

  def captureAll(target: Any, from: Any): Option[Any] = {
    None
  }
}