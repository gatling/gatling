package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.provider.assertion.AbstractAssertionProvider

import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.phase.HttpPhase

import com.ning.http.client.Response

abstract class HttpAssertion(val expression: String, val expected: String, val attrKey: Option[String], httpPhase: HttpPhase, val provider: AbstractAssertionProvider)
    extends HttpProcessor(httpPhase) {

  def assert(from: Any): (Boolean, Option[Any], Option[String]) = {
    val placeToSearch =
      from match {
        case r: Response => r.getResponseBody
        case i: Int => i
        case _ => throw new IllegalArgumentException
      }
    val providerResult = provider.assert(expected, expression, placeToSearch)
    attrKey.map {
      key =>
        (providerResult._1, providerResult._2, Some(key))
    }.getOrElse {
      (providerResult._1, providerResult._2, None)
    }
  }
}