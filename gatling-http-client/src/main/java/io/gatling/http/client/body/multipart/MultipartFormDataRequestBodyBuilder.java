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

import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;
import io.netty.handler.codec.http.HttpHeaderValues;

import java.nio.charset.Charset;
import java.util.List;

import static io.gatling.http.client.util.HttpUtils.computeMultipartBoundary;
import static io.gatling.http.client.util.HttpUtils.extractContentTypeBoundaryAttribute;
import static io.gatling.http.client.util.HttpUtils.patchContentTypeWithBoundaryAttribute;
import static io.gatling.http.client.util.MiscUtils.withDefault;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class MultipartFormDataRequestBodyBuilder extends RequestBodyBuilder.Base<List<Part<?>>> {

  public MultipartFormDataRequestBodyBuilder(List<Part<?>> content) {
    super(content);
  }

  @Override
  public RequestBody build(String contentType, Charset charset, Charset defaultCharset) {

    byte[] boundary;
    String contentTypeBoundaryAttribute = extractContentTypeBoundaryAttribute(contentType);
    if (contentTypeBoundaryAttribute != null) {
      boundary = contentTypeBoundaryAttribute.getBytes(US_ASCII);
    } else {
      boundary = computeMultipartBoundary();
      contentType = patchContentTypeWithBoundaryAttribute(withDefault(contentType, HttpHeaderValues.MULTIPART_FORM_DATA), boundary);
    }

    return new MultipartFormDataRequestBody(content, contentType, boundary);
  }
}
