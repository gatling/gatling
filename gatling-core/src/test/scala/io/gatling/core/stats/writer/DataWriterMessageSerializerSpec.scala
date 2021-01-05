/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import io.gatling.BaseSpec

class DataWriterMessageSerializerSpec extends BaseSpec {

  import DataWriterMessageSerializer._

  "sanitize" should "sanitize extra info so that simulation log format is preserved" in {
    DataWriterMessageSerializer.sanitize("\nnewlines \n are\nnot \n\n allowed\n") shouldBe " newlines   are not    allowed "
    DataWriterMessageSerializer.sanitize("\rcarriage returns \r are\rnot \r\r allowed\r") shouldBe " carriage returns   are not    allowed "
    DataWriterMessageSerializer.sanitize(
      s"${Separator}tabs $Separator are${Separator}not $Separator$Separator allowed$Separator"
    ) shouldBe " tabs   are not    allowed "
  }
}
