package io.github.gatling.udp.check

import io.gatling.commons.validation._
import io.gatling.core.check.Check.PreparedCache
import io.gatling.core.check.{Check, CheckResult}
import io.gatling.core.session.Session
import io.github.gatling.udp.UdpCheck
import io.github.gatling.udp.request.UdpMessage

trait UdpCheckSupport {
  def simpleCheck(f: UdpMessage => Boolean): UdpCheck =
    Check.Simple(
      (response: UdpMessage, _: Session, _: PreparedCache) =>
        if (f(response)) {
          CheckResult.NoopCheckResultSuccess
        } else {
          "UDP check failed".failure
        },
      None
    )
}