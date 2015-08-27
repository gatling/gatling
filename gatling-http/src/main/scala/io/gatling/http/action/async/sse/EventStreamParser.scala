/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.async.sse

import scala.annotation.tailrec

import io.gatling.commons.util.StringHelper._

object EventStreamParser {

  private final val LF = 0x0A
  private final val CR = 0x0D

  object Event {

    def unapply(cs: CharSequence, prefix: Array[Char]): Option[String] =
      if (cs.startWith(prefix)) {
        val subCs =
          if (cs.length > prefix.length + 1 && cs.charAt(prefix.length) == ' ')
            cs.subSequence(prefix.length + 1, cs.length)
          else
            cs.subSequence(prefix.length, cs.length)

        Some(subCs.toString)
      } else
        None

  }

  case object Dispatch {
    def unapply(cs: CharSequence): Option[Unit] = if (cs.contains(_ != ' ')) None else Some(Unit)
  }

  object EventName {
    val EventPrefix = "event:".toCharArray
    def unapply(cs: CharSequence): Option[String] = Event.unapply(cs, EventPrefix)
  }

  object Id {
    val IdPrefix = "id:".toCharArray
    def unapply(cs: CharSequence) = Event.unapply(cs, IdPrefix)
  }

  object Retry {
    val RetryPrefix = "retry:".toCharArray
    def unapply(cs: CharSequence) = Event.unapply(cs, RetryPrefix)
  }

  object Data {
    val DataPrefix = "data:".toCharArray
    def unapply(cs: CharSequence) = Event.unapply(cs, DataPrefix)
  }

  implicit class EventStream(val string: String) extends AnyVal {

    def eventLines: Iterator[CharSequence] = {

      val chars = ArrayCharSequence(string.unsafeChars)
      val length = string.length

      val last =
        if (string.isEmpty)
          0
        else if (length >= 2 && chars.charAt(length - 2) == CR && chars.charAt(length - 1) == LF)
          length - 2
        else if (length >= 1 && chars.charAt(length - 1) == CR || chars.charAt(length - 1) == LF)
          length - 1
        else
          length

        @tailrec
        def loop(start: Int, curr: Int, it: Iterator[CharSequence], inline: Boolean): Iterator[CharSequence] = {

          if (curr == last) {
            val newLine = if (inline) chars.subSequence(start, curr) else EmptyCharSequence
            it ++ Iterator.single(newLine)

          } else
            chars.charAt(curr) match {
              case LF | CR =>
                if (inline)
                  loop(curr + 1, curr + 1, it ++ Iterator.single(chars.subSequence(start, curr)), inline = false)
                else
                  loop(curr + 1, curr + 1, it, inline = false)

              case _ =>
                loop(start, curr + 1, it, inline = true)
            }
        }

      loop(0, 0, Iterator.empty, inline = true)
    }
  }
}

trait EventStreamParser { this: EventStreamDispatcher =>

  import EventStreamParser._

  var currentSse = ServerSentEvent()

  def parse(expression: String): Unit =
    expression.eventLines.foreach {
      case EventName(name) => currentSse = currentSse.copy(name = Some(name))
      case Id(id)          => currentSse = currentSse.copy(id = Some(id))
      case Retry(retry)    => currentSse = currentSse.copy(retry = Some(retry.toInt))
      case Data(data) =>
        val newData = currentSse.data match {
          case None          => data
          case Some(oldData) => oldData + "\n" + data
        }
        currentSse = currentSse.copy(data = Some(newData))
      case Dispatch(_) => currentSse = dispatchEvent()
      case _           =>
    }

  private def dispatchEvent(): ServerSentEvent =
    currentSse.data match {
      case None => currentSse.copy(name = None)
      case _ =>
        dispatchEventStream(currentSse.copy())
        currentSse.copy(data = None, name = None)
    }
}
