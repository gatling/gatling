package com.excilys.ebi.gatling.http.request

import com.ning.http.client.Request
import com.excilys.ebi.gatling.core.action.request.AbstractRequest

class HttpRequest(val httpRequest: Request) extends AbstractRequest {

  def getRequest: Request = httpRequest

}