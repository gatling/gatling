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

package io.gatling.http.client.body.multipart;

import io.gatling.http.client.Param;
import io.gatling.http.client.body.multipart.impl.ByteArrayPartImpl;
import io.gatling.http.client.body.multipart.impl.PartImpl;

import java.nio.charset.Charset;
import java.util.List;

public class ByteArrayPart extends FileLikePart<byte[]> {

  public ByteArrayPart(String name,
                       byte[] content,
                       Charset charset,
                       String transferEncoding,
                       String contentId,
                       String dispositionType,
                       List<Param> customHeaders,
                       String fileName,
                       String contentType) {
    super(name,
            content,
            charset,
            transferEncoding,
            contentId,
            dispositionType,
            customHeaders,
            fileName,
            contentType
    );
  }

  @Override
  public PartImpl toImpl(byte[] boundary) {
    return new ByteArrayPartImpl(this, boundary);
  }
}
