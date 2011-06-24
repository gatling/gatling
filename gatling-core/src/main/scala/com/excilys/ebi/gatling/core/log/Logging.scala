package com.excilys.ebi.gatling.core.log

import org.slf4j.{ Logger => SLFLogger, LoggerFactory => SLFLoggerFactory }

trait Logging {
  @transient
  lazy val logger = Logger(this.getClass.getName)
}

object Logger {
  def apply(logger: String) : SLFLogger = SLFLoggerFactory getLogger logger
  def apply(clazz: Class[_]): SLFLogger = apply(clazz.getName)
  def root                  : SLFLogger = apply(SLFLogger.ROOT_LOGGER_NAME)
}