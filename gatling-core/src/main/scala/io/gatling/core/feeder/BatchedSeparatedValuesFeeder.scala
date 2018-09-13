/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

import java.io.{ File, FileInputStream, InputStream }
import java.nio.charset.Charset

import io.gatling.commons.util.Arrays

object BatchedSeparatedValuesFeeder {

  def apply(file: File, separator: Char, quoteChar: Char, conversion: Option[Record[String] => Record[Any]], strategy: FeederStrategy, bufferSize: Int, charset: Charset): Feeder[Any] = {

    val streamer: InputStream => Feeder[String] = SeparatedValuesParser.stream(separator, quoteChar, charset)

    val rawFeeder = strategy match {
      case Queue    => new QueueBatchedSeparatedValuesFeeder(new FileInputStream(file), streamer)
      case Random   => new RandomBatchedSeparatedValuesFeeder(new FileInputStream(file), streamer, bufferSize)
      case Shuffle  => new ShuffleBatchedSeparatedValuesFeeder(new FileInputStream(file), streamer, bufferSize)
      case Circular => new CircularBatchedSeparatedValuesFeeder(new FileInputStream(file), streamer)
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

abstract class BatchedSeparatedValuesFeeder(is: => InputStream, streamer: InputStream => Feeder[String]) extends CloseableFeeder[String] {

  private var currentIs: InputStream = is
  protected var feeder: Feeder[String] = streamer(currentIs)

  protected def resetStream(): Unit = {
    currentIs.close()
    currentIs = is
    feeder = streamer(currentIs)
  }

  override def close(): Unit = currentIs.close()
}

class QueueBatchedSeparatedValuesFeeder(is: => InputStream, streamer: InputStream => Feeder[String]) extends BatchedSeparatedValuesFeeder(is, streamer) {

  override def hasNext: Boolean = feeder.hasNext

  override def next(): Record[String] = feeder.next()
}

class RandomBatchedSeparatedValuesFeeder(is: => InputStream, streamer: InputStream => Feeder[String], bufferSize: Int) extends BatchedSeparatedValuesFeeder(is, streamer) {

  private val buffer = new Array[Record[String]](bufferSize)
  private var index = 0
  refill()

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

  override def next(): Record[String] =
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

class ShuffleBatchedSeparatedValuesFeeder(is: => InputStream, streamer: InputStream => Feeder[String], bufferSize: Int) extends BatchedSeparatedValuesFeeder(is, streamer) {

  private val buffer = new Array[Record[String]](bufferSize)
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

  override def next(): Record[String] =
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

class CircularBatchedSeparatedValuesFeeder(is: => InputStream, streamer: InputStream => Feeder[String]) extends BatchedSeparatedValuesFeeder(is, streamer) {

  override def hasNext: Boolean = true

  override def next(): Record[String] = {
    if (!feeder.hasNext) {
      resetStream()
    }
    feeder.next()
  }
}
