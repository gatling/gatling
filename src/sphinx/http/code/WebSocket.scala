import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class WebSocket {

  //#wsName
  ws("WS Operation").wsName("myCustomName")
  //#wsName

  //#wsOpen
  exec(ws("Connect WS").open("/room/chat?username=steph"))
  //#wsOpen

  //#wsClose
  exec(ws("Close WS").close)
  //#wsClose

  //#sendText
  exec(ws("Message")
    .sendText("""{"text": "Hello, I'm ${id} and this is message ${i}!"}"""))
  //#sendText

  val myCheck = wsListen.within(30 seconds).until(1).regex("hello (.*)").saveAs("name")

  //#check-from-message
  exec(ws("Send").sendText("hello").check(myCheck))
  //#check-from-message

  //#check-from-flow
  exec(ws("Set Check").check(myCheck))
  //#check-from-flow

  //#cancel-check
  exec(ws("Cancel Check").cancelCheck)
  //#cancel-check

  //#check-example
  exec(
    ws("Send Message")
      .sendText("hello, I'm Stephane")
      .check(wsListen.within(30 seconds).until(1).regex("hello (.*)").saveAs("name"))
  )
  //#check-example

  //#reconciliate
  exec(ws("Reconciliate states").reconciliate)
  //#reconciliate

  //#chatroom-example
  val httpConf = http
    .baseURL("http://localhost:9000")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .doNotTrackHeader("1")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("Gatling2")
    .wsBaseURL("ws://localhost:9000")

  val scn = scenario("WebSocket")
    .exec(http("Home").get("/"))
    .pause(1)
    .exec(session => session.set("id", "Steph" + session.userId))
    .exec(http("Login").get("/room?username=${id}"))
    .pause(1)
    .exec(ws("Connect WS").open("/room/chat?username=${id}"))
    .pause(1)
    .repeat(2, "i") {
    exec(ws("Say Hello WS")
      .sendText("""{"text": "Hello, I'm ${id} and this is message ${i}!"}""")
      .check(wsAwait.within(30).until(1).regex(".*I'm still alive.*"))
    )
      .pause(1)
  }
    .exec(ws("Close WS").close)
  //#chatroom-example
}
