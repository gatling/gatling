package com.excilys.ebi.gatling.http.resource

import com.excilys.ebi.gatling.core.resource.Resource
import com.ning.http.client.AsyncHttpClient

class HttpClientResource(client: AsyncHttpClient) extends Resource {
  def close = client.close
}