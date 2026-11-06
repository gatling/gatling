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

package io.gatling.http.action

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/**
 * Inbound messages that no check has matched, so that a later setCheck can match a message that has already arrived, eg as a consequence of a previous action.
 * Bounded in size (oldest messages are evicted first) and in time (messages older than maxAge are discarded). A zero maxSize or maxAge disables buffering.
 *
 * Not thread-safe: must only be accessed from the connection's event loop.
 */
private[http] final class SetCheckBuffer[T](maxSize: Int, maxAge: FiniteDuration, timestamp: T => Long) {
  private val maxAgeMillis = maxAge.toMillis
  private val enabled = maxSize > 0 && maxAgeMillis > 0
  private val queue = mutable.ArrayDeque.empty[T]

  // only looks at the head: relies on the queue being ordered by timestamp, which holds as long as messages are added in arrival order
  // and restore only puts back messages returned by removeAll, which are older than any message added since
  private def evictExpired(now: Long): Unit =
    while (queue.nonEmpty && now - timestamp(queue.head) > maxAgeMillis) {
      queue.removeHead()
    }

  def add(message: T, now: Long): Unit =
    if (enabled) {
      evictExpired(now)
      queue.addOne(message)
      if (queue.lengthIs > maxSize) {
        queue.removeHead()
      }
    }

  /**
   * @return
   *   the non-expired messages, oldest first, removing all messages from the buffer
   */
  def removeAll(now: Long): List[T] =
    if (queue.isEmpty) {
      Nil
    } else {
      evictExpired(now)
      queue.removeAll().toList
    }

  /**
   * Put back messages previously returned by removeAll that didn't get consumed. They're older than any message added since, hence prepended.
   */
  def restore(messages: List[T]): Unit =
    if (messages.nonEmpty) {
      queue.prependAll(messages)
      while (queue.lengthIs > maxSize) {
        queue.removeHead()
      }
    }

  def clear(): Unit = queue.clear()

  /**
   * Replay the buffered messages, oldest first, against a sequence of checks, removing the ones that get consumed and putting back the others.
   *
   * @param initialState
   *   the state performing the first check
   * @param now
   *   the current time, used to discard expired messages
   * @param tryMessage
   *   apply the current check of a state to a message: None if the message doesn't match, Some(Left(nextState)) if the check passed and more checks remain,
   *   Some(Right(result)) if all the checks are complete or one failed
   * @param noMoreMessages
   *   the result when all messages have been replayed and checks remain
   */
  def replay[S, R](initialState: S, now: Long)(tryMessage: (S, T) => Option[Either[S, R]])(noMoreMessages: S => R): R = {
    @tailrec
    def replayRec(state: S, messages: List[T], notMatched: List[T]): R =
      messages match {
        case message :: tail =>
          tryMessage(state, message) match {
            case None =>
              replayRec(state, tail, message :: notMatched)
            case Some(Left(nextState)) =>
              // the messages that didn't match the previous check might match the next one, retry them, oldest first
              replayRec(nextState, notMatched reverse_::: tail, Nil)
            case Some(Right(result)) =>
              // restore before the caller triggers the result's afterStateUpdate, as the next action might perform a new setCheck
              restore(notMatched reverse_::: tail)
              result
          }

        case _ =>
          restore(notMatched.reverse)
          noMoreMessages(state)
      }

    replayRec(initialState, removeAll(now), Nil)
  }
}
