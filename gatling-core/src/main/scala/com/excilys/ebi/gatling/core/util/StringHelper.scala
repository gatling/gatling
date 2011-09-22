package com.excilys.ebi.gatling.core.util

import com.excilys.ebi.gatling.core.context.Context

import org.slf4j.helpers.MessageFormatter

object StringHelper {
  def interpolate(context: Context, urlToFormat: String, interpolations: Seq[String]) = {
    val urlInterpolationsNumber = interpolations.size
    if (urlInterpolationsNumber == 0)
      urlToFormat
    else if (urlInterpolationsNumber == 1)
      MessageFormatter.format(urlToFormat, context.getAttribute(interpolations.head)).getMessage
    else if (urlInterpolationsNumber == 2)
      MessageFormatter.format(urlToFormat, context.getAttribute(interpolations(0)), context.getAttribute(interpolations(1))).getMessage
    else {
      val interpolationsFromContext: Seq[String] = for (interpolation <- interpolations) yield context.getAttribute(interpolation)

      MessageFormatter.arrayFormat(urlToFormat, interpolationsFromContext.toArray).getMessage
    }
  }
}