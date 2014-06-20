/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.jms.action

import io.gatling.core.result.writer.{ DataWriterMessage, GroupMessage, RequestMessage, DataWriterClient }
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.result.message.Status
import com.typesafe.scalalogging.slf4j.StrictLogging

trait MockDataWriterClient extends DataWriterClient with StrictLogging {

  var dataWriterMsg: List[DataWriterMessage] = List()

  override def writeRequestData(session: Session,
                                requestName: String,
                                requestStartDate: Long,
                                requestEndDate: Long,
                                responseStartDate: Long,
                                responseEndDate: Long,
                                status: Status,
                                message: Option[String] = None,
                                extraInfo: List[Any] = Nil) {
    handle(RequestMessage(
      session.scenarioName,
      session.userId,
      session.groupHierarchy,
      requestName,
      requestStartDate,
      requestEndDate,
      responseStartDate,
      responseEndDate,
      status,
      message,
      extraInfo))
  }

  override def writeGroupData(session: Session, group: GroupBlock, exitDate: Long): Unit =
    handle(GroupMessage(session.scenarioName, session.userId, group, group.hierarchy, group.startDate, exitDate, group.status))

  private def handle(msg: DataWriterMessage) = {
    dataWriterMsg = msg :: dataWriterMsg
    logger.info(msg.toString)
  }

}
