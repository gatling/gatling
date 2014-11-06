package io.gatling.http.action.sse

/**
 * @author ctranxuan
 */
case class ServerSentEvent(
    data: String,
    eventType: Option[String] = None,
    id: Option[String] = None,
    retry: Option[Int] = None) {

  def asJSONString(): String = {
    val json = new StringBuilder()

    json.append("{\n")
    eventType.foreach(t => appendTo("event", t, json))
    id.foreach(i => appendTo("id", i, json))
    retry.foreach(r => appendTo("retry", r.toString, json))
    appendTo("data", data, json)
    json.append("}")

    json.toString
  }

  private def appendTo(key: String, value: String, buffer: StringBuilder): StringBuilder = {
    buffer.append(key).append(": ").append(value).append("\n")
  }
}

object ServerSentEventParser {

  def apply(expression: String): ServerSentEvent = {
      def add(sse1: ServerSentEvent, sse2: ServerSentEvent): ServerSentEvent = {
        val newData = sse1.data match {
          case "" => sse2.data
          case _  => if (sse2.data.isEmpty) sse1.data else sse1.data + "\n" + sse2.data
        }

        val newType = sse1.eventType match {
          case Some(t) => if (t == sse2.eventType.getOrElse(t)) Some(t) else throw new IllegalArgumentException(s"this and sse arguments don't have the same type: ${sse1.eventType} != ${sse2.eventType}")
          case None    => sse2.eventType
        }

        val newId = sse1.id match {
          case Some(i) => if (i == sse2.id.getOrElse(i)) Some(i) else throw new IllegalArgumentException(s"this and sse arguments don't have the same id: ${sse1.id} != ${sse2.id}")
          case None    => sse2.id
        }

        val newRetry = sse1.retry match {
          case Some(r) => if (r == sse2.retry.getOrElse(r)) Some(r) else throw new IllegalArgumentException(s"this and sse arguments don't have the same retry: ${sse1.retry} != ${sse2.retry}")
          case None    => sse2.retry
        }

        ServerSentEvent(newData, newType, newId, newRetry)
      }

    // fixme see Regexp parser combinators
    expression.lines.map(line => line.trim.toList).map {
      case 'i' :: 'd' :: ':' :: id => ServerSentEvent("", None, Some(id.mkString.trim))
      case 'e' :: 'v' :: 'e' :: 'n' :: 't' :: ':' :: event => ServerSentEvent("", Some(event.mkString.trim))
      case 'r' :: 'e' :: 't' :: 'r' :: 'y' :: ':' :: retry => ServerSentEvent("", None, None, Some(retry.mkString.trim.toInt))
      case 'd' :: 'a' :: 't' :: 'a' :: ':' :: data => ServerSentEvent(data.mkString.trim)
      case _ => ServerSentEvent("")
    }.foldLeft(ServerSentEvent(""))((sse1, sse2) => add(sse1, sse2))

  }
}