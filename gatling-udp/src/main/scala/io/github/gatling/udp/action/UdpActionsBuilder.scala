package io.github.gatling.udp.action

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.protocol.ProtocolComponentsRegistry
import io.github.gatling.udp.protocol.{UdpComponents, UdpProtocol}

abstract class UdpActionBuilder extends ActionBuilder {

  protected def components(protocolComponentsRegistry: ProtocolComponentsRegistry): UdpComponents =
    protocolComponentsRegistry.components(UdpProtocol.UdpProtocolKey)
}
