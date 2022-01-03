/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.core.stats.writer

private object RecordHeader {
  private[writer] object Run extends RecordHeader("RUN")
  private[writer] object Request extends RecordHeader("REQUEST")
  private[writer] object User extends RecordHeader("USER")
  private[writer] object Group extends RecordHeader("GROUP")
  private[writer] object Error extends RecordHeader("ERROR")
  private[writer] object Assertion extends RecordHeader("ASSERTION")
}

private sealed abstract class RecordHeader(val value: String)

sealed abstract class RawRecord(header: RecordHeader, recordLength: Int) {
  def unapply(array: Array[String]): Option[Array[String]] =
    if (array.length >= recordLength && array(0) == header.value) Some(array) else None
}

object RawRunRecord extends RawRecord(RecordHeader.Run, 6)
object RawRequestRecord extends RawRecord(RecordHeader.Request, 7)
object RawUserRecord extends RawRecord(RecordHeader.User, 4)
object RawGroupRecord extends RawRecord(RecordHeader.Group, 6)
object RawErrorRecord extends RawRecord(RecordHeader.Error, 3)
object RawAssertionRecord extends RawRecord(RecordHeader.Assertion, 2)
