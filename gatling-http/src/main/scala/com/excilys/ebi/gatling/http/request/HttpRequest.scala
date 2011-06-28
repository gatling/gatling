package com.excilys.ebi.gatling.http.request

import com.ning.http.client.Request
import com.excilys.ebi.gatling.core.action.request.AbstractRequest

class HttpRequest(givenName: String, val httpRequest: Request) extends AbstractRequest(givenName) {

  def getRequest: Request = httpRequest

  override def toString = "[HttpRequest] " + getName + " - " + getRequest.getUrl

}