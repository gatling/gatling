package io.github.gatling.udp.request

import com.softwaremill.quicklens._
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.Expression
import io.github.gatling.udp.UdpCheck
import io.github.gatling.udp.action.{UdpFireAndForgetActionBuilder, UdpSendConnectedActionBuilder}

final case class UdpDslBuilder(requestName: Expression[String]) {
  def fireAndForget: FireAndForgetDslBuilder.Message = FireAndForgetDslBuilder.Message(requestName)
  def sendConnected: SendConnectedDslBuilder.Message = SendConnectedDslBuilder.Message(requestName)
}

object FireAndForgetDslBuilder {

  final case class Message(requestName: Expression[String]) {
    def textMessage(text: Expression[String]): FireAndForgetDslBuilder = message(TextUdpMessage(text))
    def bytesMessage(bytes: Expression[Array[Byte]]): FireAndForgetDslBuilder = message(BytesUdpMessage(bytes))

    private def message(mess: UdpMessage) =
      FireAndForgetDslBuilder(
        UdpAttributes(requestName, mess, Nil),
        UdpFireAndForgetActionBuilder.apply(_)
      )
  }
}

final case class FireAndForgetDslBuilder(attributes: UdpAttributes, factory: UdpAttributes => ActionBuilder) {

  def check(checks: UdpCheck*): FireAndForgetDslBuilder = {
    require(!checks.contains(null), "Checks can't contain null elements. Forward reference issue?")
    this.modify(_.attributes.checks)(_ ::: checks.toList)
  }

  def build: ActionBuilder = factory(attributes)
}

object SendConnectedDslBuilder {

  final case class Message(requestName: Expression[String]) {
    def textMessage(text: Expression[String]): SendConnectedDslBuilder = message(TextUdpMessage(text))
    def bytesMessage(bytes: Expression[Array[Byte]]): SendConnectedDslBuilder = message(BytesUdpMessage(bytes))

    private def message(mess: UdpMessage) =
      SendConnectedDslBuilder(
        UdpAttributes(requestName, mess, Nil),
        UdpSendConnectedActionBuilder.apply(_)
      )
  }
}

final case class SendConnectedDslBuilder(attributes: UdpAttributes, factory: UdpAttributes => ActionBuilder) {

  def check(checks: UdpCheck*): SendConnectedDslBuilder = {
    require(!checks.contains(null), "Checks can't contain null elements. Forward reference issue?")
    this.modify(_.attributes.checks)(_ ::: checks.toList)
  }

  def build: ActionBuilder = factory(attributes)
}
