package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.context.Context
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object EndAction {
  val LOGGER: Logger = LoggerFactory.getLogger(classOf[EndAction]);
}
class EndAction extends Action {
  def execute(context: Context): Unit = {
    EndAction.LOGGER.info("Done user #{}", context.getUserId)
  }

  override def toString = "End Action"
}
