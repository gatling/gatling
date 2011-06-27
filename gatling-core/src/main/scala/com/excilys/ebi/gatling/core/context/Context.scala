package com.excilys.ebi.gatling.core.context

import akka.actor.Uuid

class Context(val userId: Integer, val writeActorUuid: Uuid) {
  def getUserId = userId
  def getWriteActorUuid = writeActorUuid
}