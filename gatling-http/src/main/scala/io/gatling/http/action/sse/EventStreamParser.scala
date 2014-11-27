package io.gatling.http.action.sse

import com.typesafe.scalalogging.StrictLogging

/**
 * @author ctranxuan
 */
class EventStreamParser(dispatcher: EventStreamDispatcher) extends StrictLogging {
  val idRegexp = "^id:(.*)$".r
  val eventRegexp = "^event:(.*)$".r
  val retryRegexp = "^retry:(.*)$".r
  val dataRegexp = "^data:(.*)$".r

  var currentSse = ServerSentEvent()

  def parse(expression: String): Unit = {
    expression.lines map (line => line.trim) foreach {
      case idRegexp(i)    => currentSse = currentSse.copy(id = Some(i.trim))
      case eventRegexp(n) => currentSse = currentSse.copy(name = Some(n.trim))
      case retryRegexp(r) => currentSse = currentSse.copy(retry = Some(r.trim.toInt))
      case dataRegexp(d)  => currentSse = currentSse.copy(data = if (currentSse.data.isEmpty) d.trim else currentSse.data + "\n" + d.trim)
      case ""             => dispatchEvent()
      case _              =>
    }

  }

  private def dispatchEvent(): Unit = {
    currentSse.data match {
      case "" => currentSse = currentSse.copy(name = None)
      case _ => {
        dispatcher.dispatchEventStream(currentSse.copy())
        currentSse = currentSse.copy(data = "", name = None)
      }
    }
  }

}
