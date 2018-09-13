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

package io.gatling.http.client.body.is;

import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;
import io.gatling.http.client.body.WritableContent;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.stream.ChunkedStream;

import java.io.InputStream;
import java.nio.charset.Charset;

public class InputStreamRequestBody extends RequestBody<InputStream> {

  public InputStreamRequestBody(InputStream stream, String contentType, Charset charset) {
    super(stream, contentType, charset);
  }

  @Override
  public WritableContent build(boolean zeroCopy, ByteBufAllocator alloc) {

    ChunkedStream chunkedStream = new ChunkedStream(content);

    return new WritableContent(chunkedStream, -1);
  }

  @Override
  public RequestBodyBuilder<InputStream> newBuilder() {
    return new InputStreamRequestBodyBuilder(content);
  }

  @Override
  public byte[] getBytes() {
    throw new UnsupportedOperationException("Can't read InputStream bytes without consuming it");
  }

  @Override
  public String toString() {
    return "InputStreamRequestBody{" +
      "content=" + content +
      ", contentType=" + contentType +
      ", charset=" + charset +
      '}';
  }
}
