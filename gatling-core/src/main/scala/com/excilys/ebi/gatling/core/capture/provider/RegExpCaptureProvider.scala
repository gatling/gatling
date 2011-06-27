package com.excilys.ebi.gatling.core.capture.provider

import scala.util.matching.Regex

class RegExpCaptureProvider extends AbstractCaptureProvider {
  def capture(target: Any, from: Any): Option[String] = captureOne(target, from)

  def captureOne(target: Any, from: Any): Option[String] = {
    val toBeFound = new Regex(target.toString)
    toBeFound.findFirstMatchIn(from.toString) match {
      case Some(m) => Some(m.group(1))
      case None => None
    }
  }

  def captureAll(target: Any, from: Any): Option[Any] = {
    val toBeFound = new Regex(target.toString)
    Some(toBeFound findAllIn from.toString)
  }
}
