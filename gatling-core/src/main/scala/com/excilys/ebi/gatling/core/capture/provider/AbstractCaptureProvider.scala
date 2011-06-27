package com.excilys.ebi.gatling.core.capture.provider

import com.excilys.ebi.gatling.core.log.Logging

abstract class AbstractCaptureProvider extends Logging {
  def capture(target: Any, from: Any): Option[Any]
  def captureOne(target: Any, from: Any): Option[Any]
  def captureAll(target: Any, from: Any): Option[Any]
}