package com.excilys.ebi.gatling.core.capture.provider

abstract class AbstractCaptureProvider {
  def capture(target: Any, from: Any): Option[Any]
  def captureOne(target: Any, from: Any): Option[Any]
  def captureAll(target: Any, from: Any): Option[Any]
}