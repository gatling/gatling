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

package io.gatling.core.feeder

import java.io.File
import java.nio.channels.{ FileChannel, ReadableByteChannel }

import io.gatling.commons.util.Arrays

object BatchedFeeder {
  private val BufferSize: Int = 2000

  def apply[T](
      file: File,
      feederFactory: ReadableByteChannel => Feeder[T],
      conversion: Option[Record[T] => Record[Any]],
      strategy: FeederStrategy
  ): Feeder[Any] = {

    val channelFactory = {
      val path = file.toPath
      () => FileChannel.open(path)
    }

    val rawFeeder = strategy match {
      case FeederStrategy.Queue    => new QueueBatchedFeeder[T](channelFactory, feederFactory)
      case FeederStrategy.Random   => new RandomBatchedFeeder[T](channelFactory, feederFactory, BufferSize)
      case FeederStrategy.Shuffle  => new ShuffleBatchedFeeder[T](channelFactory, feederFactory, BufferSize)
      case FeederStrategy.Circular => new CircularBatchedFeeder[T](channelFactory, feederFactory)
    }

    conversion match {
      case Some(f) =>
        val converted = rawFeeder.map(f)
        new CloseableFeeder[Any] {
          override def hasNext: Boolean = converted.hasNext
          override def next(): Record[Any] = converted.next()
          override def close(): Unit = rawFeeder.close()
        }
      case _ => rawFeeder
    }
  }
}

private sealed abstract class BatchedFeeder[T](
    channelFactory: () => ReadableByteChannel,
    feederFactory: ReadableByteChannel => Feeder[T]
) extends CloseableFeeder[T] {
  private var currentChannel: ReadableByteChannel = _
  protected var feeder: Feeder[T] = _
  reset0()

  private def reset0(): Unit = {
    currentChannel = channelFactory()
    feeder = feederFactory(currentChannel)
  }

  protected def resetStream(): Unit = {
    currentChannel.close()
    reset0()
  }

  override def close(): Unit = currentChannel.close()
}

private final class QueueBatchedFeeder[T](channelFactory: () => ReadableByteChannel, streamer: ReadableByteChannel => Feeder[T])
    extends BatchedFeeder(channelFactory, streamer) {
  override def hasNext: Boolean = feeder.hasNext

  override def next(): Record[T] = feeder.next()
}

private final class RandomBatchedFeeder[T](
    channelFactory: () => ReadableByteChannel,
    streamer: ReadableByteChannel => Feeder[T],
    bufferSize: Int
) extends BatchedFeeder(channelFactory, streamer) {
  private val buffer = new Array[Record[T]](bufferSize)
  private var index = Int.MaxValue // so refill is triggered on first access

  private def refill(): Unit = {
    var fill = 0
    while (fill < bufferSize) {
      if (!feeder.hasNext) {
        resetStream()
      }
      buffer(fill) = feeder.next()
      fill += 1
    }
    Arrays.shuffle(buffer)
  }

  override def hasNext: Boolean = true

  override def next(): Record[T] =
    if (index < bufferSize) {
      val record = buffer(index)
      index += 1
      record
    } else {
      refill()
      index = 1
      buffer(0)
    }
}

private final class ShuffleBatchedFeeder[T](
    channelFactory: () => ReadableByteChannel,
    streamer: ReadableByteChannel => Feeder[T],
    bufferSize: Int
) extends BatchedFeeder(channelFactory, streamer) {
  private val buffer = new Array[Record[T]](bufferSize)
  private var index = 0
  private var fill = 0
  refill()

  private def refill(): Unit = {
    fill = 0
    while (fill < bufferSize && feeder.hasNext) {
      buffer(fill) = feeder.next()
      fill += 1
    }
    Arrays.shuffle(buffer, fill)
  }

  override def hasNext: Boolean = index < fill || feeder.hasNext

  override def next(): Record[T] =
    if (index < fill) {
      val record = buffer(index)
      index += 1
      record
    } else {
      refill()
      assert(fill > 0, "Fill is supposed to never be 0 as we're supposed to test hasNext first")
      index = 1
      buffer(0)
    }
}

private final class CircularBatchedFeeder[T](channelFactory: () => ReadableByteChannel, streamer: ReadableByteChannel => Feeder[T])
    extends BatchedFeeder(channelFactory, streamer) {
  override def hasNext: Boolean = true

  override def next(): Record[T] = {
    if (!feeder.hasNext) {
      resetStream()
    }
    feeder.next()
  }
}
