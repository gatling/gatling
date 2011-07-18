package com.excilys.ebi.gatling.core.context

import akka.actor.Uuid

class Context(val userId: Int, val writeActorUuid: Uuid, val feederIndex: Int) {
  def getUserId = userId
  def getWriteActorUuid = writeActorUuid
  def getFeederIndex = feederIndex
}