package com.excilys.ebi.gatling.http.request

import com.ning.http.client.Request
import com.excilys.ebi.gatling.http.request.builder.HttpRequestBuilder
import com.excilys.ebi.gatling.core.action.request.AbstractRequest

class HttpRequest(givenName: String, val httpRequestBuilder: HttpRequestBuilder) extends AbstractRequest(givenName) {

  def getRequest(feederIndex: Int): Request = {
    httpRequestBuilder build (feederIndex)
  }

  override def toString = "[HttpRequest] " + getName

}