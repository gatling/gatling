package com.excilys.ebi.gatling.http.request

import com.excilys.ebi.gatling.core.action.request.AbstractRequest
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.log.Logging

import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestBuilder

import com.ning.http.client.Request

class HttpRequest(givenName: String, val httpRequestBuilder: AbstractHttpRequestBuilder[_]) extends AbstractRequest(givenName) with Logging {

  def getRequest(context: Context): Request = {
    val request = httpRequestBuilder build (context)
    logger.debug("Request created: {}", request.getUrl())
    request
  }

  override def toString = "[HttpRequest] " + getName

}