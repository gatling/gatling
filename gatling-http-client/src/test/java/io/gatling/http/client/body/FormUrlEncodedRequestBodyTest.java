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

import io.gatling.http.client.Param;
import io.gatling.http.client.body.form.FormUrlEncodedRequestBody;
import io.gatling.netty.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FormUrlEncodedRequestBodyTest {
  private void formUrlEncoding(Charset charset) throws Exception {
    String key = "key";
    String value = "中文";
    List<Param> params = new ArrayList<>();
    params.add(new Param(key, value));
    ByteBuf bb = (ByteBuf) new FormUrlEncodedRequestBody(params, TEXT_PLAIN.toString(), charset).build(ByteBufAllocator.DEFAULT).getContent();
    try {
      String ahcString = ByteBufUtils.byteBuf2String(US_ASCII, bb);
      String jdkString = key + "=" + URLEncoder.encode(value, charset.name());
      assertEquals(ahcString, jdkString);
    } finally {
      bb.release();
    }
  }

  @Test
  public void formUrlEncodingShouldSupportUtf8Charset() throws Exception {
    formUrlEncoding(UTF_8);
  }

  @Test
  public void formUrlEncodingShouldSupportNonUtf8Charset() throws Exception {
    formUrlEncoding(Charset.forName("GBK"));
  }
}
