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

package io.gatling.http.client.body.form;

import io.gatling.http.client.Param;
import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.RequestBodyBuilder;
import io.netty.handler.codec.http.HttpHeaderValues;

import java.nio.charset.Charset;
import java.util.List;

import static io.gatling.http.client.util.MiscUtils.withDefault;

public class FormUrlEncodedRequestBodyBuilder extends RequestBodyBuilder.Base<List<Param>> {

  public FormUrlEncodedRequestBodyBuilder(List<Param> content) {
    super(content);
  }

  @Override
  public RequestBody build(String contentType, Charset charset, Charset defaultCharset) {
    return new FormUrlEncodedRequestBody(content, contentType != null ? contentType : HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString(), withDefault(charset, defaultCharset));
  }
}
