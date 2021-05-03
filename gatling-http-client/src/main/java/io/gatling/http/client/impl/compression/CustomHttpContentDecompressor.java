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

package io.gatling.http.client.impl.compression;

import com.aayushatharva.brotli4j.Brotli4jLoader;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.util.AsciiString;

public class CustomHttpContentDecompressor extends HttpContentDecompressor {

  static final AsciiString BR = new AsciiString("br");

  @Override
  protected EmbeddedChannel newContentDecoder(String contentEncoding) throws Exception {
    if (Brotli4jLoader.isAvailable() && BR.contentEqualsIgnoreCase(contentEncoding)) {
      return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
        ctx.channel().config(), new BrotliDecoder());
    } else {
      return super.newContentDecoder(contentEncoding);
    }
  }

  @Override
  protected String getTargetContentEncoding(String contentEncoding) {
    return contentEncoding;
  }
}
