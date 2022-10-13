package io.github.gatling.udp.protocol

import io.gatling.core.CoreComponents
import io.gatling.core.protocol.ProtocolComponents
import io.gatling.core.session.Session
import io.github.gatling.udp.client.UdpConnection

final class UdpComponents(
  val udpProtocol: UdpProtocol,
  val udpConnection: UdpConnection,
  val coreComponents: CoreComponents,
) extends ProtocolComponents {

  override def onStart: Session => Session = Session.Identity

  override def onExit: Session => Unit = ProtocolComponents.NoopOnExit

}
