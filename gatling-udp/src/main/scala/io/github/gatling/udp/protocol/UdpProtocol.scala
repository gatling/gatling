package io.github.gatling.udp.protocol

import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.protocol.{Protocol, ProtocolKey}
import io.github.gatling.udp.client.UdpConnection

import java.net.InetAddress

object UdpProtocol extends StrictLogging {

  val UdpProtocolKey: ProtocolKey[UdpProtocol, UdpComponents] = new ProtocolKey[UdpProtocol, UdpComponents] {

    def protocolClass: Class[Protocol] = classOf[UdpProtocol].asInstanceOf[Class[Protocol]]

    def defaultProtocolValue(configuration: GatlingConfiguration): UdpProtocol =
      throw new IllegalStateException("Can't provide a default value for UdpProtocol")

    def newComponents(coreComponents: CoreComponents): UdpProtocol => UdpComponents = { udpProtocol =>
    val udpConnection = new UdpConnection()
      coreComponents.actorSystem.registerOnTermination {
        logger.trace(s"udpConnection registerOnTermination (before) isConnected : ${udpConnection.isConnected}")
        udpConnection.disconnect()
        udpConnection.close()
        logger.trace(s"udpConnection registerOnTermination (after) isConnected : ${udpConnection.isConnected}")
      }
      logger.trace(s"udpConnection (before) isConnected : ${udpConnection.isConnected}")
      udpConnection.connect(InetAddress.getByName(udpProtocol.host), udpProtocol.port)
      logger.trace(s"udpConnection (after) isConnected : ${udpConnection.isConnected}")
      new UdpComponents(udpProtocol, udpConnection, coreComponents)
    }
  }
}

final case class UdpProtocol(
    host: String,
    port: Int
) extends Protocol {

  def host(hostVal: String): UdpProtocol = copy(host = hostVal)

  def port(portVal: Int): UdpProtocol = copy(port = portVal)

  type Components = UdpComponents
}
