package com.excilys.ebi.gatling.http.request

import java.io.File

abstract class HttpRequestBody

case class StringBody(string: String) extends HttpRequestBody
case class FilePathBody(filePath: String) extends HttpRequestBody
