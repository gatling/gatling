package com.excilys.ebi.gatling.core.provider.capture

import scala.util.matching.Regex

class RegExpCaptureProvider(textContent: String) extends AbstractCaptureProvider {
  def capture(expression: Any): Option[String] = {
    logger.debug("[RegExpCaptureProvider] Capturing with expression : {}", expression)
    new Regex(expression.toString).findFirstMatchIn(textContent).map { m =>
      m.group(1)
    }
  }
}