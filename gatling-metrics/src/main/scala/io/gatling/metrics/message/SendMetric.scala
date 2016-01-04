/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.metrics.message

import java.nio.charset.StandardCharsets.UTF_8

import io.gatling.commons.util.StringHelper

import akka.util.ByteString
import com.typesafe.scalalogging.StrictLogging

private[metrics] case class GraphiteMetrics(byteString: ByteString)

private[metrics] object GraphiteMetrics extends StrictLogging {

  def apply(pathValuePairs: Iterator[(String, Long)], epoch: Long): GraphiteMetrics = {

    val sb = StringHelper.stringBuilder()
    pathValuePairs.foreach {
      case (path, value) =>
        sb.append(path).append(' ').append(value).append(' ').append(epoch).append('\n')
    }
    if (logger.underlying.isDebugEnabled)
      logger.debug(s"GraphiteMetrics=${sb.toString}")
    GraphiteMetrics(ByteString(sb.toString, UTF_8.name))
  }
}
