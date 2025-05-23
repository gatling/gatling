/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.http.client.body.multipart;

import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;
import io.gatling.http.client.body.WritableContent;
import io.gatling.http.client.body.multipart.impl.MessageEndPartImpl;
import io.gatling.http.client.body.multipart.impl.MultipartChunkedInput;
import io.gatling.http.client.body.multipart.impl.PartImpl;
import io.gatling.netty.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.handler.codec.http.HttpContent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MultipartFormDataRequestBody extends RequestBody.Base<List<Part<?>>> {

  private static final byte[] EMPTY_BYTES = new byte[0];

  private static final Logger LOGGER = LoggerFactory.getLogger(MultipartFormDataRequestBody.class);

  private final byte[] boundary;
  private final String patchedContentType;

  MultipartFormDataRequestBody(List<Part<?>> content, String patchedContentType, byte[] boundary) {
    super(content);
    this.boundary = boundary;
    this.patchedContentType = patchedContentType;
  }

  private MultipartChunkedInput toChunkedInput() {
    List<PartImpl> partImpls = new ArrayList<>(content.size() + 1);
    for (Part<?> part : content) {
      partImpls.add(part.toImpl(boundary));
    }
    partImpls.add(new MessageEndPartImpl(boundary));

    return new MultipartChunkedInput(partImpls);
  }

  @Override
  public WritableContent build(ByteBufAllocator alloc) {
    MultipartChunkedInput content = toChunkedInput();
    return new WritableContent(content, content.length());
  }

  @Override
  public RequestBodyBuilder newBuilder() {
    return new MultipartFormDataRequestBodyBuilder(content);
  }

  @Override
  public byte[] getBytes() {
    MultipartChunkedInput chunkedInput = toChunkedInput();

    CompositeByteBuf composite =
        new CompositeByteBuf(ByteBufAllocator.DEFAULT, false, Integer.MAX_VALUE);

    try {
      ByteBuf chunk;
      do {
        HttpContent content = chunkedInput.readChunk(ByteBufAllocator.DEFAULT);
        if (content != null) {
          chunk = content.content();
          composite.addComponent(true, chunk);
        } else {
          chunk = null;
        }
      } while (chunk != null);
      return ByteBufUtils.byteBuf2Bytes(composite);

    } catch (Exception e) {
      LOGGER.error("An exception occurred while getting the bytes of the parts", e);
      return EMPTY_BYTES;

    } finally {
      composite.release();
    }
  }

  @Override
  public String getPatchedContentType() {
    return patchedContentType;
  }

  @Override
  public String print(int maxLength) {
    final StringBuilder sb = new StringBuilder("MultipartFormDataRequestBody{");
    sb.append("boundary=").append(Arrays.toString(boundary));
    sb.append(", patchedContentType='").append(patchedContentType).append('\'');
    sb.append(", content=");
    content.forEach(part -> sb.append(truncate(part.toString(), maxLength)).append("\n"));
    sb.append('}');
    return sb.toString();
  }
}
