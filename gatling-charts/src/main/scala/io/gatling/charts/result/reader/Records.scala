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
package io.gatling.charts.result.reader

import io.gatling.core.result.writer.{ RawUserRecord, RawGroupRecord, RawRequestRecord }

import scala.collection.mutable

import io.gatling.core.result.Group
import io.gatling.core.result.message.{ KO, MessageEvent, Status }
import io.gatling.core.result.writer.FileDataWriter.GroupMessageSerializer

class UserRecordParser(bucketFunction: Int => Int, runStart: Long) {

  def unapply(array: Array[String]) = RawUserRecord.unapply(array).map(parseUserRecord)

  private def parseUserRecord(strings: Array[String]): UserRecord = {

    val scenario = strings(0)
    val userId = strings(1)
    val event = MessageEvent(strings(3))
    val startDate = (strings(4).toLong - runStart).toInt
    val endDate = (strings(5).toLong - runStart).toInt
    UserRecord(scenario, userId, startDate, event, bucketFunction(startDate), bucketFunction(endDate))
  }
}

class RequestRecordParser(bucketFunction: Int => Int, runStart: Long) {

  def unapply(array: Array[String]) = RawRequestRecord.unapply(array).map(parseRequestRecord)

  private def parseRequestRecord(strings: Array[String]): RequestRecord = {

    val group = {
      val groupString = strings(3)
      if (groupString.isEmpty) None else Some(GroupRecordParser.parseGroup(groupString))
    }
    val request = strings(4)

    val firstByteSentTimestamp = strings(5).toLong
    val lastByteSentTimestamp = strings(6).toLong
    val firstByteReceivedTimestamp = strings(7).toLong
    val lastByteReceivedTimestamp = strings(8).toLong

    val executionStart = (firstByteSentTimestamp - runStart).toInt
    val executionEnd = (strings(8).toLong - runStart).toInt
    val status = Status.valueOf(strings(9))
    val errorMessage = if (status == KO) Some(strings(10)) else None
    val executionStartBucket = bucketFunction(executionStart)
    val executionEndBucket = bucketFunction(executionEnd)

    val responseTime = (lastByteReceivedTimestamp - firstByteSentTimestamp).toInt
    val latency = (firstByteReceivedTimestamp - lastByteSentTimestamp).toInt

    RequestRecord(group, request, reduceAccuracy(executionStart), reduceAccuracy(executionEnd), status, executionStartBucket, executionEndBucket, responseTime, latency, errorMessage)
  }
}

object GroupRecordParser {

  val GroupCache = mutable.Map.empty[String, Group]

  def parseGroup(string: String) = GroupCache.getOrElseUpdate(string, GroupMessageSerializer.deserializeGroups(string))
}

class GroupRecordParser(bucketFunction: Int => Int, runStart: Long) {

  def unapply(array: Array[String]) = RawGroupRecord.unapply(array).map(parseGroupRecord)

  private def parseGroupRecord(strings: Array[String]): GroupRecord = {

    val group = GroupRecordParser.parseGroup(strings(3))
    val entryTimestamp = strings(4).toLong
    val exitTimestamp = strings(5).toLong
    val entryDate = (entryTimestamp - runStart).toInt
    val cumulatedResponseTime = strings(6).toInt
    val oks = strings(7).toInt
    val kos = strings(8).toInt
    val status = Status.valueOf(strings(9))
    val duration = (exitTimestamp - entryTimestamp).toInt
    val executionDateBucket = bucketFunction(entryDate)
    GroupRecord(group, reduceAccuracy(entryDate), duration, cumulatedResponseTime, oks, kos, status, executionDateBucket)
  }
}

case class RequestRecord(group: Option[Group], name: String, requestStart: Int, responseEnd: Int, status: Status, requestStartBucket: Int, responseEndBucket: Int, responseTime: Int, latency: Int, errorMessage: Option[String])
case class GroupRecord(group: Group, startDate: Int, duration: Int, cumulatedResponseTime: Int, oks: Int, kos: Int, status: Status, startDateBucket: Int)
case class UserRecord(scenario: String, userId: String, startDate: Int, event: MessageEvent, startDateBucket: Int, endDateBucket: Int)
