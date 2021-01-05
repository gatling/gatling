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

package io.gatling.http.client.test.listener;

import io.gatling.http.client.HttpListener;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.ArrayList;
import java.util.List;

public abstract class CompleteResponseListener implements HttpListener {

  protected HttpResponseStatus status;
  protected HttpHeaders headers;
  protected List<ByteBuf> chunks;

  @Override
  public void onHttpResponse(HttpResponseStatus status, HttpHeaders headers) {
    this.status = status;
    this.headers = headers;
  }

  @Override
  public void onHttpResponseBodyChunk(ByteBuf chunk, boolean last) {

    if (chunk.isReadable()) {
      if (chunks == null) {
        chunks = new ArrayList<>(1);
      }
      chunks.add(chunk.retain());
    }

    if (last) {
      onComplete();
    }
  }

  private void releaseChunks() {
    if (chunks != null) {
      for (ByteBuf chunk: chunks) {
        chunk.release();
      }
    }
  }

  @Override
  public void onThrowable(Throwable e) {
    releaseChunks();
  }

  public abstract void onComplete();
}
