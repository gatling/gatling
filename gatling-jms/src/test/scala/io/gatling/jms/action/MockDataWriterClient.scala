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

  override def writeGroupData(session: Session, group: GroupBlock, exitDate: Long) {
    handle(GroupMessage(session.scenarioName, session.userId, group, group.hierarchy, group.startDate, exitDate, group.status))
  }

  private def handle(msg: DataWriterMessage) = {
    dataWriterMsg = msg :: dataWriterMsg
    logger.info(msg.toString)
  }

}
