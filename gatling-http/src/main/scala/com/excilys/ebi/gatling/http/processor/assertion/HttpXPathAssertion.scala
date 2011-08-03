package com.excilys.ebi.gatling.http.processor.assertion

import com.excilys.ebi.gatling.core.provider.assertion.XPathAssertionProvider

import com.excilys.ebi.gatling.http.phase.HttpPhase

import com.ning.http.client.Response

class HttpXPathAssertion(expression: String, expected: String, attrKey: Option[String], httpPhase: HttpPhase)
    extends HttpAssertion(expression, expected, attrKey, httpPhase, null) {

  override def toString = "HttpXPathAssertion ('" + expression + "' must be equal to '" + expected + "')"

  override def equals(that: Any) = {
    if (!that.isInstanceOf[HttpXPathAssertion])
      false
    else {
      val other = that.asInstanceOf[HttpXPathAssertion]
      this.expression == other.expression && this.expected == other.expected
    }
  }

  override def hashCode = this.expression.size + this.expected.size

  override def assertInRequest(from: Any, identifier: String): (Boolean, Option[Any], Option[String]) = {
    val placeToSearch =
      from match {
        case r: Response => r.getResponseBodyAsBytes
        case _ => throw new IllegalArgumentException
      }
    val providerResult = new XPathAssertionProvider(identifier).assert(expected, expression, placeToSearch)
    attrKey.map {
      key =>
        (providerResult._1, providerResult._2, Some(key))
    }.getOrElse {
      (providerResult._1, providerResult._2, None)
    }
  }
}