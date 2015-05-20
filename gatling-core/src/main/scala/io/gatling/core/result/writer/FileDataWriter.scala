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
package io.gatling.core.result.writer

import java.io.RandomAccessFile
import java.nio.charset.CharsetEncoder
import java.nio.{ CharBuffer, ByteBuffer }
import java.nio.channels.FileChannel

import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.core.assertion.Assertion
import io.gatling.core.config.GatlingFiles.simulationLogDirectory
import io.gatling.core.util.StringHelper._
import io.gatling.core.util.PathHelper._

object FileDataWriter {

  val Separator = '\t'

  object SanitizableString {
    val SanitizerPattern = """[\n\r\t]""".r
  }

  implicit class SanitizableString(val string: String) extends AnyVal {

    /**
     * Converts whitespace characters that would break the simulation log format into spaces.
     */
    def sanitize = SanitizableString.SanitizerPattern.replaceAllIn(string, " ")
  }

  sealed trait DataWriterMessageSerializer[T] {

    def serializeGroups(groupHierarchy: List[String]): Fastring = groupHierarchy.mkFastring(",")

    def serialize(m: T): Fastring
  }

  implicit val RunMessageSerializer = new DataWriterMessageSerializer[RunMessage] {

    def serialize(runMessage: RunMessage): Fastring = {
      import runMessage._
      val description = if (runDescription.isEmpty) " " else runDescription
      fast"${RunRecordHeader.value}$Separator$simulationClassName$Separator$simulationId$Separator$start$Separator$description${Separator}2.0$Eol"
    }
  }

  implicit val UserMessageSerializer = new DataWriterMessageSerializer[UserMessage] {

    def serialize(user: UserMessage): Fastring = {
      import user._
      fast"${UserRecordHeader.value}$Separator${session.scenario}$Separator${session.userId}$Separator${event.name}$Separator${session.startDate}$Separator$date$Eol"
    }
  }

  implicit val ResponseMessageSerializer = new DataWriterMessageSerializer[ResponseMessage] {

    private def serializeExtraInfo(extraInfo: List[Any]): Fastring =
      extraInfo.map(info => fast"$Separator${info.toString.sanitize}").mkFastring

    private def serializeMessage(message: Option[String]): String =
      message match {
        case Some(m) => m.sanitize
        case None    => " "
      }

    def serialize(response: ResponseMessage): Fastring = {
      import response._
      import timings._
      fast"${RequestRecordHeader.value}$Separator$scenario$Separator$userId$Separator${serializeGroups(groupHierarchy)}$Separator$name$Separator$requestStartDate$Separator$requestEndDate$Separator$responseStartDate$Separator$responseEndDate$Separator$status$Separator${serializeMessage(message)}${serializeExtraInfo(extraInfo)}$Eol"
    }
  }

  implicit val GroupMessageSerializer = new DataWriterMessageSerializer[GroupMessage] {

    def serialize(group: GroupMessage): Fastring = {
      import group._
      fast"${GroupRecordHeader.value}$Separator$scenario$Separator$userId$Separator${serializeGroups(groupHierarchy)}$Separator$startDate$Separator$endDate$Separator$cumulatedResponseTime$Separator$status$Eol"
    }
  }

  implicit val AssertionSerializer = new DataWriterMessageSerializer[Assertion] {

    def serialize(assertion: Assertion): Fastring = fast"${AssertionRecordHeader.value}$Separator${assertion.serialized}$Eol"
  }

  implicit val ErrorMessageSerializer = new DataWriterMessageSerializer[ErrorMessage] {

    def serialize(error: ErrorMessage): Fastring = {
      import error._
      fast"${GroupRecordHeader.value}$Separator$message$Separator$date$Eol"
    }
  }
}

case class FileData(limit: Int, buffer: ByteBuffer, encoder: CharsetEncoder, channel: FileChannel) extends DataWriterData

/**
 * File implementation of the DataWriter
 *
 * It writes the data of the simulation if a tabulation separated values file
 */
class FileDataWriter extends DataWriter[FileData] {

  import FileDataWriter._

  def onInit(init: Init): FileData = {

    import init._

    val simulationLog = simulationLogDirectory(runMessage.runId)(configuration) / "simulation.log"

    val limit = configuration.data.file.bufferSize

    val channel = new RandomAccessFile(simulationLog.toFile, "rw").getChannel

    val data = FileData(limit, ByteBuffer.allocate(limit * 2), configuration.core.charset.newEncoder, channel)

    system.registerOnTermination(channel.close())
    assertions.foreach(assertion => push(assertion, data))
    push(runMessage, data)

    data
  }

  override def onFlush(data: FileData): Unit = {}

  private def flush(data: FileData): Unit = {
    import data._
    buffer.flip()
    while (buffer.hasRemaining)
      channel.write(buffer)
    buffer.clear()
  }

  private def push[T](message: T, data: FileData)(implicit serializer: DataWriterMessageSerializer[T]): Unit = {

    import data._

    val fs = serializer.serialize(message)

    for (string <- fs)
      encoder.encode(CharBuffer.wrap(string.unsafeChars), buffer, false)
    if (buffer.position >= limit)
      flush(data)
  }

  override def onMessage(message: LoadEventMessage, data: FileData): Unit = message match {
    case user: UserMessage         => push(user, data)
    case group: GroupMessage       => push(group, data)
    case response: ResponseMessage => push(response, data)
    case error: ErrorMessage       => push(error, data)
    case _                         =>
  }

  override def onCrash(cause: String, data: FileData): Unit = {}

  override def onTerminate(data: FileData): Unit = {
    flush(data)
    data.channel.force(true)
  }
}
