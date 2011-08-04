package com.excilys.ebi.gatling.http.provider.assertion

import com.excilys.ebi.gatling.core.provider.assertion.AbstractAssertionProvider

import com.excilys.ebi.gatling.http.provider.capture.HttpHeadersCaptureProvider

class HttpHeadersAssertionProvider extends AbstractAssertionProvider {
  def assert(expected: String, target: Any, from: Any): (Boolean, Option[Any]) = {
    val results = (new HttpHeadersCaptureProvider).capture(target, from)

    logger.debug(" -- Header Assertion - results: {}", results)

    var res: Boolean = false
    var value: Option[String] = None

    results.map { r =>
      r match {
        case s: String => if (s == expected) {
          res = true
          value = Some(s)
        }
        case list: java.util.List[String] =>
          val it = list.iterator
          while (it.hasNext) {
            val r = it.next
            if (r == expected) {
              res = true
              value = Some(r)
            }
          }
      }
    }
    (res, value)
  }
}