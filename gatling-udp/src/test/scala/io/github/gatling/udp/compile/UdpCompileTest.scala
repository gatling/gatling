package io.github.gatling.udp.compile

import io.gatling.core.Predef._
import io.github.gatling.udp.Predef._
import io.github.gatling.udp.request.{BytesUdpMessage, TextUdpMessage}

import java.nio.charset.StandardCharsets

class UdpCompileTest extends Simulation {

  private val udpProtocol = udp("localhost", 12000)

  private val scn = scenario("FireAndForget")
    .exec { session =>
      println(session)
      session
    }
    .pause(1)
    .exec(
      udp("1 fireAndForget text")
        .fireAndForget.textMessage("fireAndForget text").build
    )
    .pause(1)
    .exec(
      udp("2 fireAndForget text into byte")
        .fireAndForget.bytesMessage("fireAndForget text into byte".getBytes(StandardCharsets.UTF_8)).build
    )
    .pause(1)
    .exec(
      udp("3 fireAndForget byte (hello world)")
        .fireAndForget.bytesMessage(Array[Byte](0x68, 0x65, 0x6C, 0x6C, 0x6F, 0x20, 0x77, 0x6F, 0x72, 0x6C, 0x64)).build
    )
    .pause(1)
    .exec(
      udp("1 sendConnected text")
        .sendConnected.textMessage("sendConnected text").build
    )
    .pause(1)
    .exec(
      udp("2 sendConnected text into byte")
        .sendConnected.bytesMessage("sendConnected text into byte".getBytes(StandardCharsets.UTF_8)).build
    )
    .pause(1)
    .exec(
      udp("3 sendConnected byte (hello world)")
        .sendConnected.bytesMessage(Array[Byte](0x68, 0x65, 0x6C, 0x6C, 0x6F, 0x20, 0x77, 0x6F, 0x72, 0x6C, 0x64)).build
    )
    .pause(1)

  setUp(scn.inject(atOnceUsers(1))).protocols(udpProtocol)
}
