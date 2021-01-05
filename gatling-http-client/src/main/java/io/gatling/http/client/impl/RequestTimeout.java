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

package io.gatling.http.client.impl;

import io.gatling.http.client.HttpListener;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface RequestTimeout {

  static RequestTimeout requestTimeout(long timeout, HttpListener listener) {
    return timeout > 0 ? new DefaultRequestTimeout(timeout, listener) : NoopRequestTimeout.INSTANCE;
  }

  void start(EventLoop eventLoop);

  boolean isDone();

  void cancel();

  void setChannel(Channel channel);

  class DefaultRequestTimeout implements RequestTimeout {
    private final long timeout;
    private final HttpListener listener;
    private Channel channel;
    private InetSocketAddress remoteAddress;
    private ScheduledFuture<?> f;

    private DefaultRequestTimeout(long timeout, HttpListener listener) {
      this.timeout = timeout;
      this.listener = listener;
    }

    @Override
    public void start(EventLoop eventLoop) {
      f = eventLoop.schedule(this::execute, timeout, TimeUnit.MILLISECONDS);
    }

    private void execute() {
      listener.onThrowable(new RequestTimeoutException(timeout, remoteAddress));
      if (channel != null) {
        channel.close();
      }
    }

    public boolean isDone() {
      return f.isDone();
    }

    public void cancel() {
      f.cancel(true);
    }

    public void setChannel(Channel channel) {
      this.channel = channel;
      remoteAddress = (InetSocketAddress) channel.remoteAddress();
    }
  }

  class NoopRequestTimeout implements RequestTimeout {

    private static final NoopRequestTimeout INSTANCE = new NoopRequestTimeout();

    private NoopRequestTimeout() {

    }

    @Override
    public void start(EventLoop eventLoop) {
    }

    @Override
    public boolean isDone() {
      return false;
    }

    @Override
    public void cancel() {
    }

    @Override
    public void setChannel(Channel channel) {
    }
  }
}
