package io.gatling.http.funspec

import io.gatling.core.Predef._
import io.gatling.core.config.Protocol
import io.gatling.core.funspec.GatlingFunSpec
import io.gatling.http.Predef._
import io.gatling.http.config.HttpProtocolBuilder

abstract class GatlingHttpFunSpec extends GatlingFunSpec {

  /** The base URL to make HTTP requests against */
  def baseURL: String

  /** HTTP protocol configuration. Use this to add headers and other http configuration. */
  def httpConf: HttpProtocolBuilder = http
    .baseURL(baseURL)
    .acceptHeader("application/json, text/html, text/plain, */*")
    .acceptEncodingHeader("gzip, deflate")

  override def protocolConf: Protocol = httpConf

}

