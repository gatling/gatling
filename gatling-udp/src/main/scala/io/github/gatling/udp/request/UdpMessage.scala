package io.github.gatling.udp.request

import io.gatling.commons.validation.Validation
import io.gatling.core.session.{Expression, Session}

sealed trait UdpMessage {
  private[udp] def udpMessage(session: Session): Validation[Array[Byte]]
}

final case class BytesUdpMessage(bytes: Expression[Array[Byte]]) extends UdpMessage {
  override private[udp] def udpMessage(session: Session): Validation[Array[Byte]] =
    bytes(session).map {identity}
}

final case class TextUdpMessage(txt: Expression[String]) extends UdpMessage {
  override private[udp] def udpMessage(session: Session): Validation[Array[Byte]] =
    txt(session).map(s => s.getBytes())
}
