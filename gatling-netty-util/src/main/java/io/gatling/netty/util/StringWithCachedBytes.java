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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.util.List;

public class StringWithCachedBytes {

  public static ByteBuf toByteBuf(List<StringWithCachedBytes> chunks) {
    switch (chunks.size()) {
      case 0:
        return Unpooled.EMPTY_BUFFER;
      case 1:
        return Unpooled.wrappedBuffer(chunks.get(0).bytes);
      default:
        CompositeByteBuf comp = new CompositeByteBuf(ByteBufAllocator.DEFAULT, false, chunks.size());
        for (StringWithCachedBytes chunk : chunks) {
          comp.addComponent(true, Unpooled.wrappedBuffer(chunk.bytes));
        }
        return comp;
    }
  }

  public static String toString(List<StringWithCachedBytes> chunks) {
    StringBuilder sb = StringBuilderPool.DEFAULT.get();
    for (StringWithCachedBytes chunk: chunks) {
      sb.append(chunk.string);
    }
    return sb.toString();
  }

  public final String string;
  public final byte[] bytes;

  public StringWithCachedBytes(String string, Charset charset) {
    this.string = string;
    this.bytes = string.getBytes(charset);
  }

  @Override
  public String toString() {
    return string;
  }
}
