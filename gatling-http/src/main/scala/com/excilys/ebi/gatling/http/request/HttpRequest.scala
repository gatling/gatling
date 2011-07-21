package com.excilys.ebi.gatling.http.request

import com.ning.http.client.Request
import com.excilys.ebi.gatling.http.request.builder.HttpRequestBuilder
import com.excilys.ebi.gatling.core.action.request.AbstractRequest
import com.excilys.ebi.gatling.core.context.Context

class HttpRequest(givenName: String, val httpRequestBuilder: HttpRequestBuilder) extends AbstractRequest(givenName) {

  private var request: Request = null

  def getRequest(context: Context): Request = {
    request = httpRequestBuilder build (context)
    request
  }

  def getRequest = request

  override def toString = "[HttpRequest] " + getName

}