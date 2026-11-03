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

package io.gatling.http.client;

import io.netty.handler.codec.http2.Http2CodecUtil;

/**
 * The HTTP/2 flow-control windows the client advertises for receiving data, which cap how many
 * bytes the server can send per round trip before waiting for a WINDOW_UPDATE.
 */
public final class Http2FlowControlWindows {

  /** Same as Chrome: Netty's (and the HTTP/2 spec's) 64KB defaults throttle downloads. */
  public static final int DEFAULT_STREAM_WINDOW_SIZE = 6 * 1024 * 1024;

  public static final int DEFAULT_CONNECTION_WINDOW_SIZE = 15 * 1024 * 1024;

  public static final Http2FlowControlWindows DEFAULT =
      new Http2FlowControlWindows(DEFAULT_STREAM_WINDOW_SIZE, DEFAULT_CONNECTION_WINDOW_SIZE);

  private final int streamWindowSize;
  private final int connectionWindowSize;

  /**
   * @param streamWindowSize advertised with SETTINGS_INITIAL_WINDOW_SIZE, applies to each stream
   * @param connectionWindowSize applies to all the streams of a connection, can't be lower than the
   *     65,535 bytes every connection starts with
   */
  public Http2FlowControlWindows(int streamWindowSize, int connectionWindowSize) {
    if (streamWindowSize <= 0) {
      throw new IllegalArgumentException(
          "HTTP/2 stream window size must be positive: " + streamWindowSize);
    }
    if (connectionWindowSize < Http2CodecUtil.DEFAULT_WINDOW_SIZE) {
      throw new IllegalArgumentException(
          "HTTP/2 connection window size can't be lower than "
              + Http2CodecUtil.DEFAULT_WINDOW_SIZE
              + ": "
              + connectionWindowSize);
    }
    this.streamWindowSize = streamWindowSize;
    this.connectionWindowSize = connectionWindowSize;
  }

  public int getStreamWindowSize() {
    return streamWindowSize;
  }

  public int getConnectionWindowSize() {
    return connectionWindowSize;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Http2FlowControlWindows that = (Http2FlowControlWindows) o;
    return streamWindowSize == that.streamWindowSize
        && connectionWindowSize == that.connectionWindowSize;
  }

  @Override
  public int hashCode() {
    return 31 * streamWindowSize + connectionWindowSize;
  }

  @Override
  public String toString() {
    return "Http2FlowControlWindows{streamWindowSize="
        + streamWindowSize
        + ", connectionWindowSize="
        + connectionWindowSize
        + '}';
  }
}
