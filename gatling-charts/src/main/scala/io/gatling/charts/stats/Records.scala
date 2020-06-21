/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.charts.stats

import scala.collection.mutable

import io.gatling.commons.stats.{ Group, KO, Status }
import io.gatling.core.stats.message.MessageEvent
import io.gatling.core.stats.writer.{ RawErrorRecord, RawGroupRecord, RawRequestRecord, RawUserRecord }

private object UserRecordParser {

  def unapply(array: Array[String]): Option[UserRecord] = RawUserRecord.unapply(array).map(parseUserRecord)

  private def parseUserRecord(strings: Array[String]): UserRecord = {

    val scenario = strings(1)
    val userId = strings(2)
    val event = MessageEvent(strings(3))
    val start = strings(4).toLong
    val end = if (event == MessageEvent.End) strings(5).toLong else 0

    UserRecord(scenario, userId, event, start, end)
  }
}

private class RequestRecordParser(bucketFunction: Long => Int) {

  def unapply(array: Array[String]): Option[RequestRecord] = RawRequestRecord.unapply(array).map(parseRequestRecord)

  private def parseRequestRecord(strings: Array[String]): RequestRecord = {

    val group = {
      val groupString = strings(2)
      if (groupString.isEmpty) None else Some(GroupRecordParser.parseGroup(groupString))
    }
    val request = strings(3)

    val start = strings(4).toLong
    val end = strings(5).toLong

    val status = Status.apply(strings(6))
    val errorMessage = if (status == KO) Some(strings(7)) else None

    if (end != Long.MinValue) {
      // regular request
      RequestRecord(group, request, status, start, bucketFunction(start), bucketFunction(end), (end - start).toInt, errorMessage, incoming = false)
    } else {
      // unmatched incoming event
      RequestRecord(group, request, status, start, bucketFunction(start), bucketFunction(start), 0, errorMessage, incoming = true)
    }
  }
}

private object GroupRecordParser {

  val GroupCache = mutable.Map.empty[String, Group]

  def parseGroup(string: String): Group = GroupCache.getOrElseUpdate(string, Group(string.split(",").toList))
}

private class GroupRecordParser(bucketFunction: Long => Int) {

  def unapply(array: Array[String]): Option[GroupRecord] = RawGroupRecord.unapply(array).map(parseGroupRecord)

  private def parseGroupRecord(strings: Array[String]): GroupRecord = {

    val group = GroupRecordParser.parseGroup(strings(2))
    val start = strings(3).toLong
    val end = strings(4).toLong
    val cumulatedResponseTime = strings(5).toInt
    val status = Status.apply(strings(6))
    val duration = (end - start).toInt
    GroupRecord(group, duration, cumulatedResponseTime, status, start, bucketFunction(start))
  }
}

private object ErrorRecordParser {

  def unapply(array: Array[String]): Option[ErrorRecord] = RawErrorRecord.unapply(array).map(parseErrorRecord)

  private def parseErrorRecord(strings: Array[String]): ErrorRecord = {

    val message = strings(1)
    val timestamp = strings(2).toLong

    ErrorRecord(message, timestamp)
  }
}

private final case class RequestRecord(
    group: Option[Group],
    name: String,
    status: Status,
    start: Long,
    startBucket: Int,
    endBucket: Int,
    responseTime: Int,
    errorMessage: Option[String],
    incoming: Boolean
)
private final case class GroupRecord(group: Group, duration: Int, cumulatedResponseTime: Int, status: Status, start: Long, startBucket: Int)
private final case class UserRecord(scenario: String, userId: String, event: MessageEvent, start: Long, end: Long)
private final case class ErrorRecord(message: String, timestamp: Long)
