package io.github.gatling

import io.gatling.core.check.Check
import io.github.gatling.udp.request.UdpMessage

package object udp {

  type UdpCheck = Check[UdpMessage]
}
