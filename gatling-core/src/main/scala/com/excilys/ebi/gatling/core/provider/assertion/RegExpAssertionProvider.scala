package com.excilys.ebi.gatling.core.provider.assertion

import com.excilys.ebi.gatling.core.provider.capture.RegExpCaptureProvider

class RegExpAssertionProvider extends AbstractAssertionProvider {
  def assert(expected: Any, target: Any, from: Any) = {
    val result = (new RegExpCaptureProvider).capture(target, from)
    (result == expected, result)
  }
}
