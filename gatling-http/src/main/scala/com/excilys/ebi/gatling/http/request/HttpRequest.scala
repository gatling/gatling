package com.excilys.ebi.gatling.http.request

import com.ning.http.client.Request
import com.excilys.ebi.gatling.http.request.builder.HttpRequestBuilder
import com.excilys.ebi.gatling.core.action.request.AbstractRequest
import com.excilys.ebi.gatling.core.context.Context

class HttpRequest(givenName: String, val httpRequestBuilder: HttpRequestBuilder) extends AbstractRequest(givenName) {

  def getRequest(context: Context): Request = {
    httpRequestBuilder build (context)
  }

  override def toString = "[HttpRequest] " + getName

}