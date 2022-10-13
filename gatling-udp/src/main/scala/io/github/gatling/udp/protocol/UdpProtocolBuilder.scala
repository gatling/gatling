package io.github.gatling.udp.protocol

case object UdpProtocolBuilder {

  def toUdpProtocol(builder: UdpProtocolBuilder): UdpProtocol = builder.build

}

final case class UdpProtocolBuilder(
    host: String,
    port: Int
) {
  def build: UdpProtocol = UdpProtocol(
      host,
      port,
    )
}