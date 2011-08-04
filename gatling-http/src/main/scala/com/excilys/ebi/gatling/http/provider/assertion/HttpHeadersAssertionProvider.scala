package com.excilys.ebi.gatling.http.provider.assertion

import com.excilys.ebi.gatling.core.provider.assertion.AbstractAssertionProvider

import com.excilys.ebi.gatling.http.provider.capture.HttpHeadersCaptureProvider

class HttpHeadersAssertionProvider extends AbstractAssertionProvider {
  def assert(expected: String, target: Any, from: Any): (Boolean, Option[Any]) = {
    val results = (new HttpHeadersCaptureProvider).capture(target, from)

    var res: Boolean = false
    var value: Option[String] = None
    for (result <- results.getOrElse(Nil).asInstanceOf[List[String]]) {
      if (result == expected) {
        res = true
        value = Some(result)
      }
    }
    (res, value)
  }
}