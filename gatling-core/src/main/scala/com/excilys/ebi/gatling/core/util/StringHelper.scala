package com.excilys.ebi.gatling.core.util
import org.slf4j.helpers.MessageFormatter
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.log.Logging

object StringHelper extends Logging {
  def interpolateString(context: Context, urlToFormat: String, interpolations: Seq[String]) = {

    interpolations.size match {
      case 0 => urlToFormat
      case 1 => MessageFormatter.format(urlToFormat, context.getAttribute(interpolations(0))).getMessage
      case 2 => MessageFormatter.format(urlToFormat, context.getAttribute(interpolations(0)), context.getAttribute(interpolations(1))).getMessage
      case _ => {
        val interpolationsFromContext: Seq[String] = for (interpolation <- interpolations) yield context.getAttribute(interpolation)
        MessageFormatter.arrayFormat(urlToFormat, interpolationsFromContext.toArray).getMessage
      }
    }
  }

  // Used in scenarios
  def interpolate(urlToFormat: String, interpolations: String*) = (c: Context) => interpolateString(c, urlToFormat, interpolations)
}