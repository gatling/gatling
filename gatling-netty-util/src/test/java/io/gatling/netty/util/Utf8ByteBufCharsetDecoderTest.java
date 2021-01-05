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

package io.gatling.netty.util;

import java.util.Arrays;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class Utf8ByteBufCharsetDecoderTest {

  @Test
  void testByteBuf2BytesHasBackingArray() {
    byte[] inputBytes = "testdata".getBytes(US_ASCII);
    ByteBuf buf = Unpooled.wrappedBuffer(inputBytes);
    try {
      byte[] output = ByteBufUtils.byteBuf2Bytes(buf);
      assertArrayEquals(inputBytes, output);
    } finally {
      buf.release();
    }
  }

  @Test
  void testByteBuf2BytesNoBackingArray() {
    byte[] inputBytes = "testdata".getBytes(US_ASCII);
    ByteBuf buf = Unpooled.directBuffer();
    try {
      buf.writeBytes(inputBytes);
      byte[] output = ByteBufUtils.byteBuf2Bytes(buf);
      assertArrayEquals(inputBytes, output);
    } finally {
      buf.release();
    }
  }

  @Test
  void byteBufs2StringShouldBeAbleToDealWithCharsWithVariableBytesLength() {
    String inputString = "°ä–";
    byte[] inputBytes = inputString.getBytes(UTF_8);

    for (int i = 1; i < inputBytes.length - 1; i++) {
      ByteBuf buf1 = Unpooled.wrappedBuffer(inputBytes, 0, i);
      ByteBuf buf2 = Unpooled.wrappedBuffer(inputBytes, i, inputBytes.length - i);
      try {
        String output = ByteBufUtils.byteBuf2String(UTF_8, buf1, buf2);
        assertEquals(inputString, output);
      } finally {
        buf1.release();
        buf2.release();
      }
    }
  }

  @Test
  void byteBufs2StringShouldBeAbleToDealWithBrokenCharsTheSameWayAsJavaImpl() {
    String inputString = "foo 加特林岩石 bar";
    byte[] inputBytes = inputString.getBytes(UTF_8);

    int droppedBytes = 1;

    for (int i = 1; i < inputBytes.length - 1 - droppedBytes; i++) {
      byte[] part1 = Arrays.copyOfRange(inputBytes, 0, i);
      byte[] part2 = Arrays.copyOfRange(inputBytes, i + droppedBytes, inputBytes.length);
      byte[] merged = new byte[part1.length + part2.length];
      System.arraycopy(part1, 0, merged, 0, part1.length);
      System.arraycopy(part2, 0, merged, part1.length, part2.length);

      ByteBuf buf1 = Unpooled.wrappedBuffer(part1);
      ByteBuf buf2 = Unpooled.wrappedBuffer(part2);
      try {
        String output = ByteBufUtils.byteBuf2String(UTF_8, buf1, buf2);
        String javaString = new String(merged, UTF_8);
        assertNotEquals(inputString, output);
        assertEquals(javaString, output);
      } finally {
        buf1.release();
        buf2.release();
      }
    }
  }
}
