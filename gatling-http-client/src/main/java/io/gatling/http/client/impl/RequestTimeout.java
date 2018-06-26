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

package io.gatling.http.client.impl;

import io.gatling.http.client.HttpListener;
import io.gatling.netty.util.ahc.StringBuilderPool;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface RequestTimeout {

  class RequestTimeoutException extends TimeoutException {

    private static String message(long timeout, InetSocketAddress remoteAddress) {
      StringBuilder message = StringBuilderPool.DEFAULT.get().append("Request timeout");
      if (remoteAddress != null) {
        message.append(" to ").append(remoteAddress.getHostName());
        if (!remoteAddress.isUnresolved()) {
          message.append('/').append(remoteAddress.getAddress().getHostAddress());
        }
        message.append(':').append(remoteAddress.getPort());
      }
      return message.append(" after ").append(timeout).append(" ms").toString();
    }

    RequestTimeoutException(long timeout, InetSocketAddress remoteAddress) {
      super(message(timeout, remoteAddress));
    }

    @Override
    public Throwable fillInStackTrace() {
      return this;
    }
  }

  static RequestTimeout scheduleRequestTimeout(long timeout, HttpListener listener, EventLoop eventLoop) {
    return timeout > 0 ? new DefaultRequestTimeout(timeout, listener, eventLoop) : NoopRequestTimeout.INSTANCE;
  }

  boolean isDone();

  void cancel();

  void setChannel(Channel channel);

  class DefaultRequestTimeout implements RequestTimeout {
    private final long timeout;
    private final ScheduledFuture<?> f;
    private final HttpListener listener;
    private Channel channel;
    private InetSocketAddress remoteAddress;

    private DefaultRequestTimeout(long timeout, HttpListener listener, EventLoop eventLoop) {
      this.timeout = timeout;
      this.listener = listener;
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
