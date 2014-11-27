package io.gatling.http.action.sse

/**
 * @author ctranxuan
 */
case class ServerSentEvent(
    data: String = "",
    name: Option[String] = None,
    id: Option[String] = None,
    retry: Option[Int] = None) {

  def asJSONString(): String = {
    val json = new StringBuilder()

    json.append("{\n")
    name.foreach(t => appendTo("event", t, json))
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
