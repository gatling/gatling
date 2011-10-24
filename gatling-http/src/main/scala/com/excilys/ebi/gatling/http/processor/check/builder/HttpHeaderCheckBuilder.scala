package com.excilys.ebi.gatling.http.processor.check.builder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.CheckType
import com.excilys.ebi.gatling.core.processor.builtin.EqualityCheckType
import com.excilys.ebi.gatling.core.processor.builtin.NonEqualityCheckType
import com.excilys.ebi.gatling.core.processor.builtin.ExistenceCheckType
import com.excilys.ebi.gatling.core.processor.builtin.NonExistenceCheckType
import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.check.HttpHeaderCheck
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.core.util.StringHelper._

object HttpHeaderCheckBuilder {
	def headerEquals(headerNameFormatter: Context => String, expected: String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(expected), Some(EMPTY), Some(EqualityCheckType))
	def headerEquals(headerName: String, expected: String): HttpHeaderCheckBuilder = headerEquals((c: Context) => headerName, expected)

	def headerNotEquals(headerNameFormatter: Context => String, expected: String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(expected), Some(EMPTY), Some(NonEqualityCheckType))
	def headerNotEquals(headerName: String, expected: String): HttpHeaderCheckBuilder = headerNotEquals((c: Context) => headerName, expected)

	def headerExists(headerNameFormatter: Context => String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(EMPTY), Some(EMPTY), Some(ExistenceCheckType))
	def headerExists(headerName: String): HttpHeaderCheckBuilder = headerExists((c: Context) => headerName)

	def headerNotExists(headerNameFormatter: Context => String) = new HttpHeaderCheckBuilder(Some(headerNameFormatter), Some(EMPTY), Some(EMPTY), Some(NonExistenceCheckType))
	def headerNotExists(headerName: String): HttpHeaderCheckBuilder = headerNotExists((c: Context) => headerName)
}
class HttpHeaderCheckBuilder(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], checkType: Option[CheckType])
		extends HttpCheckBuilder[HttpHeaderCheckBuilder](expressionFormatter, expected, attrKey, Some(HeadersReceived), checkType) {

	def newInstance(expressionFormatter: Option[Context => String], expected: Option[String], attrKey: Option[String], httpPhase: Option[HttpPhase], checkType: Option[CheckType]) = {
		new HttpHeaderCheckBuilder(expressionFormatter, expected, attrKey, checkType)
	}

	def build: HttpCheck = new HttpHeaderCheck(expressionFormatter.get, expected.get, attrKey.get, checkType.get)
}
