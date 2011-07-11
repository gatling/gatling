package com.excilys.ebi.gatling.http.request.builder

import com.ning.http.client.Request

abstract class HttpRequestBuilder {
  def build: Request
}