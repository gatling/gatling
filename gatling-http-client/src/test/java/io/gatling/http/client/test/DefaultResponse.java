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

package io.gatling.http.client.test;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

public class DefaultResponse<T> implements Response<T> {

  private final HttpResponseStatus status;
  private final HttpHeaders headers;
  private final T body;

  public DefaultResponse(HttpResponseStatus status, HttpHeaders headers, T body) {
    this.status = status;
    this.headers = headers;
    this.body = body;
  }

  @Override
  public HttpResponseStatus status() {
    return status;
  }

  @Override
  public HttpHeaders headers() {
    return headers;
  }

  @Override
  public T body() {
    return body;
  }

  @Override
  public String toString() {
    return "DefaultResponse{" +
            "status=" + status +
            ", headers=" + headers +
            ", body=" + body +
            '}';
  }
}
