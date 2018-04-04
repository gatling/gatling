/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.http.client.body;

import static io.gatling.http.client.ahc.util.HttpUtils.*;
import static io.gatling.http.client.ahc.util.MiscUtils.*;

import io.gatling.http.client.body.part.Part;
import io.gatling.http.client.body.part.impl.MessageEndPartImpl;
import io.gatling.http.client.body.part.impl.MultipartChunkedInput;
import io.gatling.http.client.body.part.impl.MultipartFileRegion;
import io.gatling.http.client.body.part.impl.PartImpl;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class MultipartFormDataRequestBody extends RequestBody<List<Part<?>>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MultipartFormDataRequestBody.class);

  public MultipartFormDataRequestBody(List<Part<?>> content) {
    super(content);
  }

  @Override
  public WritableContent build(String contentTypeHeader, Charset charset, boolean zeroCopy, ByteBufAllocator alloc) {

    byte[] boundary;
    String contentTypeOverride;
    String contentTypeBoundaryAttribute = extractContentTypeBoundaryAttribute(contentTypeHeader);
    if (contentTypeBoundaryAttribute != null) {
      boundary = contentTypeBoundaryAttribute.getBytes(US_ASCII);
      contentTypeOverride = null;
    } else {
      boundary = computeMultipartBoundary();
      contentTypeOverride = patchContentTypeWithBoundaryAttribute(withDefault(contentTypeHeader, HttpHeaderValues.MULTIPART_FORM_DATA), boundary);
    }

    List<PartImpl<?>> partImpls = new ArrayList<>(content.size() + 1);
    for (Part<?> part: content) {
      partImpls.add(part.toImpl(boundary));
    }
    partImpls.add(new MessageEndPartImpl(boundary));

    long contentLength = computeContentLength(partImpls);

    Object content = zeroCopy ? new MultipartFileRegion(partImpls, contentLength): new MultipartChunkedInput(partImpls, contentLength);

    return new WritableContent(content, contentLength, contentTypeOverride);
  }

  private long computeContentLength(List<PartImpl<?>> partImpls) {
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
}
