package com.excilys.ebi.gatling.http.request

trait Param
case class StringParam(val string: String) extends Param
case class ContextParam(val string: String) extends Param