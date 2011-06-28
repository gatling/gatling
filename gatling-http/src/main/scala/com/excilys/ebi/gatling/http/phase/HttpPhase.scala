package com.excilys.ebi.gatling.http.phase

sealed trait HttpPhase
class StatusReceived extends HttpPhase {
  override def equals(that: Any): Boolean = {
    that.isInstanceOf[StatusReceived]
  }

  override def hashCode = 1

  override def toString = "StatusReceived"
}
class HeadersReceived extends HttpPhase {
  override def equals(that: Any): Boolean = {
    that.isInstanceOf[HeadersReceived]
  }

  override def hashCode = 2

  override def toString = "HeadersReceived"
}
class BodyPartReceived extends HttpPhase {
  override def equals(that: Any): Boolean = {
    that.isInstanceOf[BodyPartReceived]
  }

  override def hashCode = 3

  override def toString = "BodyPartReceived"
}
class CompletePageReceived extends HttpPhase {
  override def equals(that: Any): Boolean = {
    that.isInstanceOf[CompletePageReceived]
  }

  override def hashCode = 4

  override def toString = "CompletePageReceived"
}