package com.excilys.ebi.gatling.http.phase

sealed trait HttpResponseHook
case object StatusReceived extends HttpResponseHook
case object HeadersReceived extends HttpResponseHook
case object BodyPartReceived extends HttpResponseHook
case object CompletePageReceived extends HttpResponseHook