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

package io.gatling.http.client.body;

import io.netty.buffer.ByteBufAllocator;

import java.io.IOException;

public interface RequestBody {

  String getContentType();

  WritableContent build(ByteBufAllocator alloc) throws IOException;

  RequestBodyBuilder newBuilder();

  byte[] getBytes();

  abstract class Base<T> implements RequestBody {

    protected final T content;
    protected final String contentType;

    public Base(T content, String contentType) {
      this.content = content;
      this.contentType = contentType;
    }

    public T getContent() {
      return content;
    }

    @Override
    public String getContentType() {
      return contentType;
    }
  }
}
