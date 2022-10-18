package io.github.gatling.udp

import io.gatling.core.session._
import io.github.gatling.udp.check.UdpCheckSupport
import io.github.gatling.udp.protocol.{UdpProtocol, UdpProtocolBuilder}
import io.github.gatling.udp.request.{FireAndForgetDslBuilder, SendConnectedDslBuilder, UdpDslBuilder}

import scala.language.implicitConversions

trait UdpDsl extends UdpCheckSupport {

  def udp(host: String, port: Int): UdpProtocol = new UdpProtocolBuilder(host, port).build

  def udp(requestName: Expression[String]): UdpDslBuilder = UdpDslBuilder(requestName)

  implicit def udpProtocolBuilder2udpProtocol(builder: UdpProtocolBuilder): UdpProtocol = builder.build

  implicit def udpDslBuilder2ActionBuilder(builder: FireAndForgetDslBuilder): ActionBuilder = builder.build

  implicit def udpDslBuilder2ActionBuilder(builder: SendConnectedDslBuilder): ActionBuilder = builder.build

}
