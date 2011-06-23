package com.excilys.ebi.gatling.http.phase

sealed trait HttpResponseHook
class StatusReceived extends HttpResponseHook {
  override def equals(that: Any): Boolean = {
    that.isInstanceOf[StatusReceived]
  }

  override def hashCode = 1
}
class HeadersReceived extends HttpResponseHook {
  override def equals(that: Any): Boolean = {
    that.isInstanceOf[HeadersReceived]
  }

  override def hashCode = 2
}
class BodyPartReceived extends HttpResponseHook {
  override def equals(that: Any): Boolean = {
    that.isInstanceOf[BodyPartReceived]
  }

  override def hashCode = 3
}
class CompletePageReceived extends HttpResponseHook {
  override def equals(that: Any): Boolean = {
    that.isInstanceOf[CompletePageReceived]
  }

  override def hashCode = 4
}