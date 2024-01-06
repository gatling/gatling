/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.netty.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

public final class ByteBufUtilsTest {

  @Test
  void byteBuf2BytesShouldBeAbleToExtractMultipleByteBufInstances() {
    ByteBuf buffer1 = Unpooled.buffer(3);
    ByteBuf buffer2 = Unpooled.buffer(5);

    try {
      buffer1.writeBytes(new byte[] {0, 1, 2});
      buffer2.writeBytes(new byte[] {0, 1, 2, 3, 4});
      assertArrayEquals(
          new byte[] {0, 1, 2, 0, 1, 2, 3, 4}, ByteBufUtils.byteBufs2Bytes(buffer1, buffer2));
    } finally {
      buffer1.release();
      buffer2.release();
    }
  }
}
