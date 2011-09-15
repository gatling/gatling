package com.excilys.ebi.gatling.core.provider.capture

import scala.util.matching.Regex
import com.excilys.ebi.gatling.core.log.Logging

object RegExpCaptureProvider extends Logging {

  var captureProviderPool: Map[String, RegExpCaptureProvider] = Map.empty

  def prepare(identifier: String, textContent: String) = {
    captureProviderPool += (identifier -> new RegExpCaptureProvider(textContent))
  }

  def capture(identifier: String, expression: Any) = {
    logger.debug("[RegExpCaptureProvider] Capturing with expression : {}", expression)
    captureProviderPool.get(identifier).get.capture(expression)
  }

  def clear(identifier: String) = {
    captureProviderPool -= identifier
  }

}
class RegExpCaptureProvider(textContent: String) extends AbstractCaptureProvider {
  def capture(expression: Any): Option[String] = {
    new Regex(expression.toString).findFirstMatchIn(textContent).map { m =>
      m.group(1)
    }
  }
}