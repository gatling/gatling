package com.excilys.ebi.gatling.core.context

import akka.actor.Uuid

class Context(val userId: Int, val writeActorUuid: Uuid) {
  def getUserId = userId
  def getWriteActorUuid = writeActorUuid
}