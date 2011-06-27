package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.context.Context

class EndAction extends Action {
  def execute(context: Context): Unit = {
    logger.info("Done user #{}", context.getUserId)
  }

  override def toString = "End Action"
}
