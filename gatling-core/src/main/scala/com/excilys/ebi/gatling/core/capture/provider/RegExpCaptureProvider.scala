package com.excilys.ebi.gatling.core.capture.provider

import scala.util.matching.Regex

class RegExpCaptureProvider extends AbstractCaptureProvider {
  def capture(target: Any, from: Any): Option[String] = captureOne(target, from)

  def captureOne(target: Any, from: Any): Option[String] = {
    val toBeFound = new Regex(target.toString)
    toBeFound findFirstIn from.toString
  }

  def captureAll(target: Any, from: Any): Option[Any] = {
    val toBeFound = new Regex(target.toString)
    Some(toBeFound findAllIn from.toString)
  }
}