package io.github.gatling.udp.action

import com.softwaremill.quicklens.ModifyPimp
import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen
import io.github.gatling.udp.UdpCheck
import io.github.gatling.udp.protocol.{UdpComponents, UdpProtocol}
import io.github.gatling.udp.request.UdpAttributes

case class UdpFireAndForgetActionBuilder(
    attributes: UdpAttributes,
) extends ActionBuilder with NameGen with StrictLogging {

  def check(checks: UdpCheck*): UdpFireAndForgetActionBuilder =
    this.modify(_.attributes.checks).using(_ ::: checks.toList)

  override def build(ctx: ScenarioContext, next: Action): Action = {

    logger.trace(s"UdpFireAndForgetAction build")
    val udpComponents: UdpComponents = ctx.protocolComponentsRegistry.components(UdpProtocol.UdpProtocolKey)

    new UdpFireAndForgetAction(
      attributes,
      udpComponents,
      next,
      attributes.message,
      ctx.coreComponents.throttler.filter(_ => ctx.throttled),
    )
  }

}
