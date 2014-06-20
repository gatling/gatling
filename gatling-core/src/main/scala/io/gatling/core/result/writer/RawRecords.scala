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
package io.gatling.core.result.writer

sealed trait RecordHeader { def value: String }
object RunRecordHeader extends RecordHeader { val value = "RUN" }
object RequestRecordHeader extends RecordHeader { val value = "REQUEST" }
object UserRecordHeader extends RecordHeader { val value = "USER" }
object GroupRecordHeader extends RecordHeader { val value = "GROUP" }

sealed abstract class RawRecord(header: RecordHeader, recordLength: Int) {
  def unapply(array: Array[String]) =
    if (array.length >= recordLength && array(2) == header.value) Some(array) else None
}

object RawRunRecord extends RawRecord(RunRecordHeader, 5)
object RawRequestRecord extends RawRecord(RequestRecordHeader, 10)
object RawUserRecord extends RawRecord(UserRecordHeader, 5)
object RawGroupRecord extends RawRecord(GroupRecordHeader, 7)
