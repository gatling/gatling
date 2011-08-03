package com.excilys.ebi.gatling.core.provider.assertion

import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider

class XPathAssertionProvider(identifier: String) extends AbstractAssertionProvider {
  def assert(expected: String, target: Any, from: Any) = {
    val result = XPathCaptureProvider.getInstance(identifier).capture(target, from)
    result.map {
      r =>
        (r == expected, Some(r))
    }.getOrElse {
      (false, None)
    }
  }
}