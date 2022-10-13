package io.github.gatling.udp

import io.gatling.core.session._
import io.github.gatling.udp.check.UdpCheckSupport
import io.github.gatling.udp.protocol.{UdpProtocol, UdpProtocolBuilder}
import io.github.gatling.udp.request.UdpDslBuilder

trait UdpDsl extends UdpCheckSupport {

  def udp(host: String, port: Int): UdpProtocol = new UdpProtocolBuilder(host, port).build

  def udp(requestName: Expression[String]): UdpDslBuilder = UdpDslBuilder(requestName)

}
