package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.context.Context

class EndAction extends Action {
  def execute(context: Context): Unit = {
    println("Done user " + context.getUserId)
  }
}