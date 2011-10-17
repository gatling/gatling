package com.excilys.ebi.gatling.http.request

object HttpPhase extends Enumeration {
	type HttpPhase = Value
	val StatusReceived, HeadersReceived, BodyPartReceived, CompletePageReceived = Value
}