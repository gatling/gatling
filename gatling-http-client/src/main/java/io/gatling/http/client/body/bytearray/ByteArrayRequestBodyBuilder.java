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

package io.gatling.http.client.body.bytearray;

import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;

import java.nio.charset.Charset;

import static io.gatling.http.client.util.MiscUtils.withDefault;

public class ByteArrayRequestBodyBuilder extends RequestBodyBuilder.Base<byte[]> {

  private final String fileName;

  public ByteArrayRequestBodyBuilder(byte[] content, String fileName) {
    super(content);
    this.fileName = fileName;
  }

  @Override
  public RequestBody build(String contentType, Charset charset, Charset defaultCharset) {
    return new ByteArrayRequestBody(content, contentType, fileName, withDefault(charset, defaultCharset));
  }
}
