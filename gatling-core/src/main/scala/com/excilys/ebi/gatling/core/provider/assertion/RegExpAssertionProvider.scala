package com.excilys.ebi.gatling.core.provider.assertion

import com.excilys.ebi.gatling.core.provider.capture.RegExpCaptureProvider

class RegExpAssertionProvider extends AbstractAssertionProvider {
  val captureProvider = new RegExpCaptureProvider

  def assert(expected: String, target: Any, from: Any) = {
    val result = captureProvider.capture(target, from)
    (result.getOrElse(None) == expected, result)
  }
}
