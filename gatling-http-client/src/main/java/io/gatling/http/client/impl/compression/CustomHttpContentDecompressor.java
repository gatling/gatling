/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpContentDecompressor;

public final class CustomHttpContentDecompressor extends HttpContentDecompressor {

  // HTTP/1.1 channels are used by one request at a time: this flag is set by HttpAppHandler before
  // each request write, so it never has to be reset when the channel is put back in the pool
  private boolean enabled = true;

  public CustomHttpContentDecompressor() {
    super(0);
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  protected EmbeddedChannel newContentDecoder(String contentEncoding) throws Exception {
    // returning null makes HttpContentDecoder pass the message through untouched
    return enabled ? super.newContentDecoder(contentEncoding) : null;
  }

  @Override
  protected String getTargetContentEncoding(String contentEncoding) {
    return contentEncoding;
  }
}
