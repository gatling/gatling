/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.action.sse

import com.typesafe.scalalogging.StrictLogging

object EventStreamParser {

  // FIXME remove starting whitespace here
  val IdRegexp = "^id:(.*)$".r
  val EventRegexp = "^event:(.*)$".r
  // FIXME capture must be a number
  val RetryRegexp = "^retry:(.*)$".r
  val DataRegexp = "^data:(.*)$".r
}

trait EventStreamParser extends StrictLogging { this: EventStreamDispatcher =>

  import EventStreamParser._

  var currentSse = ServerSentEvent()

  def parse(expression: String): Unit = {
    expression.lines map (line => line.trim) foreach {
      // FIXME Too many trims
      case IdRegexp(i)    => currentSse = currentSse.copy(id = Some(i.trim))
      case EventRegexp(n) => currentSse = currentSse.copy(name = Some(n.trim))
      case RetryRegexp(r) => currentSse = currentSse.copy(retry = Some(r.trim.toInt))
      case DataRegexp(d) =>
        // FIXME do we really need to update if data is empty ?
        currentSse = currentSse.copy(data =
        currentSse.data match {
          case Some("") => Some(d.trim)
          case None     => Some(d.trim)
          case _        => Some(currentSse.data + "\n" + d.trim)
        })
      case "" => dispatchEvent()
      case _  =>
    }
  }

  private def dispatchEvent(): Unit = {
    currentSse.data match {
      case None     => currentSse = currentSse.copy(name = None)
      // FIXME see above: would we really have an empty line?
      case Some("") => currentSse = currentSse.copy(name = None)
      case _ =>
        dispatchEventStream(currentSse.copy())
        currentSse = currentSse.copy(data = Some(""), name = None)
    }
  }
}
