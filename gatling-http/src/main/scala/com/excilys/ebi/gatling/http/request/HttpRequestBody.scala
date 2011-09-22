package com.excilys.ebi.gatling.http.request

abstract class HttpRequestBody

case class StringBody(string: String) extends HttpRequestBody
case class FilePathBody(filePath: String) extends HttpRequestBody
case class TemplateBody(tplPath: String, values: Map[String, Param]) extends HttpRequestBody
