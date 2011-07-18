package com.excilys.ebi.gatling.core.context

import akka.actor.Uuid

class Context(val userId: Int, val writeActorUuid: Uuid, val feederIndex: Int, data: Map[String, String]) {
  def getUserId = userId
  def getWriteActorUuid = writeActorUuid
  def getFeederIndex = feederIndex

  def getData = data

  def getElapsedActionTime =
    data.get("gatlingElapsedTime").map { l =>
      l.asInstanceOf[Long]
    }.getOrElse(0L)
}