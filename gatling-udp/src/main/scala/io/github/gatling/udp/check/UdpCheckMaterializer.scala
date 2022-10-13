package io.github.gatling.udp.check

import io.gatling.core.check.{CheckMaterializer, Preparer}
import io.github.gatling.udp.UdpCheck
import io.github.gatling.udp.request.UdpMessage

class UdpCheckMaterializer[T, P](
    override val preparer: Preparer[UdpMessage, P]
) extends CheckMaterializer[T, UdpCheck, UdpMessage, P](identity)

object UdpCheckMaterializer {

}
