package com.excilys.ebi.gatling.core.provider.capture

import scala.util.matching.Regex

class RegExpCaptureProvider extends AbstractCaptureProvider {
  def captureOne(target: Any, from: Any): Option[String] = {
    val toBeFound = new Regex(target.toString)

    toBeFound.findFirstMatchIn(from.toString).map { m =>
      m.group(1)
    }
  }

  def captureAll(target: Any, from: Any): Option[Any] = {
    val toBeFound = new Regex(target.toString)
    Some(toBeFound findAllIn from.toString)
  }
}
