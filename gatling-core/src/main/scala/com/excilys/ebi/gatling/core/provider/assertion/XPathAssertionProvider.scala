package com.excilys.ebi.gatling.core.provider.assertion

import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider

class XPathAssertionProvider extends AbstractAssertionProvider {
  def assert(expected: String, target: Any, from: Any) = {
    val result = (new XPathCaptureProvider).capture(target, from)
    result.map {
      r =>
        (r == expected, Some(r))
    }.getOrElse {
      (false, None)
    }
  }
}