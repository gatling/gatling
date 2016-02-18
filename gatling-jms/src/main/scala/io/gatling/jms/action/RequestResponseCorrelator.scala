/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.jms.action

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }
import java.util.concurrent.{ ConcurrentHashMap, Executors, TimeUnit }
import javax.jms.Message

import akka.actor.ActorRef
import com.typesafe.scalalogging.LazyLogging
import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.TimeHelper._
import io.gatling.commons.validation.Failure
import io.gatling.core.Predef._
import io.gatling.core.check.Check
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.jms._

/**
 * Created by abrahamkiggundu on 18/02/2016.
 */
object RequestResponseCorrelator extends LazyLogging {

  import scala.collection.JavaConverters._

  var statsEngine: StatsEngine = null
  val messageReceiveFailed: AtomicBoolean = new AtomicBoolean(false)
  val msgsOut: AtomicInteger = new AtomicInteger(0)
  val msgsIn: AtomicInteger = new AtomicInteger(0)
  val totalActorNotifications: AtomicInteger = new AtomicInteger(0)
  val sentMessages = new ConcurrentHashMap[String, (Long, List[JmsCheck], Session, ActorRef, String)].asScala
  val receivedMessages = new ConcurrentHashMap[String, (Long, Message)].asScala
  val correlator = Executors.newScheduledThreadPool(6)

  val correlationActivity = new Runnable {
    override def run(): Unit = {
      if (statsEngine != null) {
        //go through the received messages and if any are found corresponding to the sent then
        //pump them out for processing and remove them from both lists
        if (messageReceiveFailed.get()) {
          logger.debug("!!!!!!!!!!!!!!!!Message Receive Filure set and needs to be processed!!!!!!!!!!!!!!!! sent: " + sentMessages.size + " received: " + receivedMessages.size)
          for ((key, value) <- sentMessages) {
            logger.debug("=======>Processing a sent message: " + key)
            receivedMessages.get(key) match {
              case Some((receivedDate, message)) => {
                logger.debug("=======>Recieved message found for sent message key: " + key)
                sentMessages.get(key) match {
                  case Some((startDate, checks, session, next, title)) => {
                    logger.debug("=======>Sent message matches a received message: " + key)
                    processMessage(session, startDate, receivedDate, checks, message, next, title)
                    sentMessages.remove(key)
                    receivedMessages.remove(key)
                  }
                  case None => {
                    logger.debug("=======>No sent message matches the received messge: " + key)
                    //should never get here
                  }
                }
              }
              case None => {
                logger.debug("=======>No Recieved message found for sent message key: " + key)
                //clean out the sent message as timed out
                sentMessages.get(key) match {
                  case Some((startDate, checks, session, next, title)) => {
                    logger.debug("=======>... but we still have the sent message... must have been a timeout: " + key)
                    processMessage(session, startDate, System.currentTimeMillis, checks, null, next, title)
                    sentMessages.remove(key)
                  }
                  case None => {
                    logger.debug("=======>... where did the sent message go: " + key)
                    //should never get here
                  }
                }

              }
            }
          }
        } else {
          logger.debug(":-)    All good     :-)")
          for ((key, value) <- sentMessages) {

            receivedMessages.get(key) match {
              case Some((receivedDate, message)) => {
                logger.debug("=======>Processing a received message: " + key)
                sentMessages.get(key) match {
                  case Some((startDate, checks, session, next, title)) => {
                    logger.debug("=======>Received message matches sent message: " + key)
                    processMessage(session, startDate, receivedDate, checks, message, next, title)
                    sentMessages.remove(key)
                    receivedMessages.remove(key)
                  }
                  case None => {
                    logger.debug("=======>!!!Received Does not match sent message: " + key)
                    //should never get here
                  }
                }
              }
              case None => {
                logger.debug("No received message for this key: " + key)
                //might receive the message later so do nothing for now
              }
            }
          }
        }
      }
    }
  }

  correlator.scheduleWithFixedDelay(correlationActivity, 5, 5, TimeUnit.SECONDS)

  def processMessage(
    session:      Session,
    startDate:    Long,
    receivedDate: Long,
    checks:       List[JmsCheck],
    message:      Message,
    next:         ActorRef,
    title:        String
  ): Unit = {

      def executeNext(updatedSession: Session, status: Status, message: Option[String] = None) = {
        val timings = ResponseTimings(startDate, receivedDate)
        statsEngine.logResponse(updatedSession, title, timings, status, None, message)
        next ! updatedSession.logGroupRequest(timings.responseTime, status).increaseDrift(nowMillis - receivedDate)
      }

    // run all the checks, advise the Gatling API that it is complete and move to next
    val (checkSaveUpdate, error) = Check.check(message, session, checks)
    val newSession = checkSaveUpdate(session)
    error match {
      case None                   => executeNext(newSession, OK)
      case Some(Failure(message)) => executeNext(newSession.markAsFailed, KO, Some(message))
    }
  }

  def entryProcessor(key: String, value: String) = {

  }

}
