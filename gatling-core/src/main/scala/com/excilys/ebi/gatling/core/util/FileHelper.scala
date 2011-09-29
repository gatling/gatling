package com.excilys.ebi.gatling.core.util
import org.apache.commons.lang3.StringUtils

object FileHelper {
  val COMMA_SEPARATOR = ","
  val SEMICOLON_SEPARATOR = ";"
  val TABULATION_SEPARATOR = "\t"
  val CSV_EXTENSION = ".csv"
  val SSV_EXTENSION = ".ssv"
  val TSV_EXTENSION = ".tsv"
  val SCALA_EXTENSION = ".scala"
  val SSP_EXTENSION = ".ssp"
  val HTML_EXTENSION = ".html"

  def formatToFilename(s: String) = {
    StringUtils.stripAccents(
      s.replace("-", "_")
        .replace(" ", "_")
        .replace("'", "")
        .toLowerCase)
  }
}