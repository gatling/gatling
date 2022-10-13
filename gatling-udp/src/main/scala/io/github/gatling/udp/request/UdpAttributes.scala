package io.github.gatling.udp.request

import io.gatling.core.session.Expression
import io.github.gatling.udp.UdpCheck

object UdpAttributes {
  def apply(
     requestName: Expression[String],
     message: UdpMessage,
  ): UdpAttributes =
    new UdpAttributes(
      requestName,
      message,
      checks = Nil
    )
}

final case class UdpAttributes(
    requestName: Expression[String],
    message: UdpMessage,
    checks: List[UdpCheck],
)
