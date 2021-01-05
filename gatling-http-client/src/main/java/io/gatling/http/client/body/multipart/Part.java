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

package io.gatling.http.client.body.multipart;

import io.gatling.http.client.Param;
import io.gatling.http.client.body.multipart.impl.PartImpl;

import java.nio.charset.Charset;
import java.util.List;

public abstract class Part<T> {

  private final T content;

  private final String name;

  private final Charset charset;

  private final String transferEncoding;

  private final String contentId;

  private final String dispositionType;

  private final String contentType;

  private final List<Param> customHeaders;

  Part(String name,
                 T content,
                 Charset charset,
                 String transferEncoding,
                 String contentId,
                 String dispositionType,
                 String contentType,
                 List<Param> customHeaders) {
    this.name = name;
    this.content = content;
    this.charset = charset;
    this.transferEncoding = transferEncoding;
    this.contentId = contentId;
    this.dispositionType = dispositionType;
    this.contentType = contentType;
    this.customHeaders = customHeaders;
  }

  public T getContent() {
    return content;
  }

  public String getName() {
    return name;
  }

  public Charset getCharset() {
    return charset;
  }

  public String getTransferEncoding() {
    return transferEncoding;
  }

  public String getContentId() {
    return contentId;
  }

  public String getDispositionType() {
    return dispositionType;
  }

  public String getContentType() {
    return contentType;
  }

  public List<Param> getCustomHeaders() {
    return customHeaders;
  }

  public abstract PartImpl toImpl(byte[] boundary);

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Part{");
    sb.append("name='").append(name).append('\'');
    sb.append(", charset=").append(charset);
    sb.append(", transferEncoding='").append(transferEncoding).append('\'');
    sb.append(", contentId='").append(contentId).append('\'');
    sb.append(", dispositionType='").append(dispositionType).append('\'');
    sb.append(", contentType='").append(contentType).append('\'');
    if (customHeaders != null) {
      sb.append(", customHeaders=");
      customHeaders.forEach(customHeader ->
        sb.append(customHeader.getName()).append(": ").append(customHeader.getValue()).append(", ")
      );
      if (!customHeaders.isEmpty()) {
        sb.setLength(sb.length() - 2);
      }
    }
    sb.append(", content=").append(content);
    sb.append('}');
    return sb.toString();
  }
}
