package com.excilys.ebi.gatling.http.request.builder

import com.ning.http.client.Request
import com.excilys.ebi.gatling.core.context.Context

abstract class HttpRequestBuilder {
  def build(feederIndex: Int): Request
}