/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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
import io.gatling.http.client.body.multipart.impl.MultipartFileRegion;
import io.gatling.http.client.body.multipart.impl.PartImpl;
import io.netty.buffer.ByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultipartFormDataRequestBody extends RequestBody<List<Part<?>>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MultipartFormDataRequestBody.class);

  private final byte[] boundary;

  MultipartFormDataRequestBody(List<Part<?>> content, String contentType, Charset charset, byte[] boundary) {
    super(content, contentType, charset);
    this.boundary = boundary;
  }

  @Override
  public WritableContent build(boolean zeroCopy, ByteBufAllocator alloc) {

    List<PartImpl> partImpls = new ArrayList<>(content.size() + 1);
    for (Part<?> part: content) {
      partImpls.add(part.toImpl(boundary));
    }
    partImpls.add(new MessageEndPartImpl(boundary));

    long contentLength = computeContentLength(partImpls);

    Object content = zeroCopy ? new MultipartFileRegion(partImpls, contentLength): new MultipartChunkedInput(partImpls, contentLength);

    return new WritableContent(content, contentLength);
  }

  @Override
  public RequestBodyBuilder<List<Part<?>>> newBuilder() {
    return new MultipartFormDataRequestBodyBuilder(content);
  }

  private static long computeContentLength(List<PartImpl> partImpls) {
    try {
      long total = 0;
      for (PartImpl part : partImpls) {
        long l = part.length();
        if (l < 0) {
          return -1;
        }
        total += l;
      }
      return total;
    } catch (Exception e) {
      LOGGER.error("An exception occurred while getting the length of the parts", e);
      return 0L;
    }
  }

  @Override
  public byte[] getBytes() {
    throw new UnsupportedOperationException("MultipartFormDataRequestBody#getBytes isn't implemented yet. Contributions welcome.");
  }

  @Override
  public String toString() {
    return "MultipartFormDataRequestBody{" +
      "boundary=" + Arrays.toString(boundary) +
      ", content=" + content +
      ", contentType=" + contentType +
      ", charset=" + charset +
      '}';
  }
}
