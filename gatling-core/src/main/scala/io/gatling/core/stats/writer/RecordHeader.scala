/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

private[gatling] object RecordHeader {
  private[gatling] object Run extends RecordHeader(0)
  private[gatling] object Request extends RecordHeader(1)
  private[gatling] object User extends RecordHeader(2)
  private[gatling] object Group extends RecordHeader(3)
  private[gatling] object Error extends RecordHeader(4)
}

private[gatling] sealed abstract class RecordHeader(val value: Byte)
