package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.context.Context

class EndAction extends AbstractAction {
  def execute(context: Context): Unit = {
    if (context.getUserId == 0) {
      self.stop
    } else {
      println("Done user " + context.getUserId)
    }
  }
}