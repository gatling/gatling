package io.gatling.http.action.sse

import java.io.Closeable

import com.ning.http.client.ListenableFuture

/**
 * @author ctranxuan
 */
class SseSource(val future: ListenableFuture[Unit]) extends Closeable {
  override def close(): Unit = {
    future.done
  }
}
