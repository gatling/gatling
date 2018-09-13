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

package io.gatling.http.client.body;

import io.netty.buffer.ByteBufAllocator;

import java.io.IOException;
import java.nio.charset.Charset;

public abstract class RequestBody<T> {

  protected final T content;
  protected final String contentType;
  protected final Charset charset;

  public RequestBody(T content, String contentType, Charset charset) {
    this.content = content;
    this.contentType = contentType;
    this.charset = charset;
  }

  public T getContent() {
    return content;
  }

  public String getContentType() {
    return contentType;
  }

  public abstract WritableContent build(boolean zeroCopy, ByteBufAllocator alloc) throws IOException;

  public abstract RequestBodyBuilder<T> newBuilder();

  public abstract byte[] getBytes();
}
