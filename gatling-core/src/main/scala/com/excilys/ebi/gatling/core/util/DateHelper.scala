package com.excilys.ebi.gatling.core.util

import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

object DateHelper {

  private val resultDateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  private val fileNameDateTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmmss")

  def parseResultDate(string: String) = DateTime.parse(string, resultDateTimeFormat);

  def printResultDate(dateTime: DateTime) = resultDateTimeFormat.print(dateTime)

  def printFileNameDate(dateTime: DateTime) = fileNameDateTimeFormat.print(dateTime)
}