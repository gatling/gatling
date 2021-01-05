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

package io.gatling.commons.util

import io.gatling.BaseSpec
import io.gatling.commons.util.ByteBufs.byteBufsToByteArray

import io.netty.buffer.{ ByteBuf, Unpooled }

class ByteBufsSpec extends BaseSpec {

  it should "be able to extract data from multiple ByteBuf instances" in {
    val buffer1: ByteBuf = Unpooled.buffer(3)
    val buffer2: ByteBuf = Unpooled.buffer(5)

    try {
      buffer1.writeBytes(Array[Byte](0, 1, 2))
      buffer2.writeBytes(Array[Byte](0, 1, 2, 3, 4))

      byteBufsToByteArray(Seq(buffer1, buffer2)) shouldBe Array[Byte](0, 1, 2, 0, 1, 2, 3, 4)
    } finally {
      buffer1.release()
      buffer2.release()
    }
  }
}
