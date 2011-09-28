package com.excilys.ebi.gatling.core.action
import com.excilys.ebi.gatling.core.context.Context

class IfAction(testFunction: Context => Boolean, nextTrue: Action, nextFalse: Option[Action], nextAfter: Action) extends Action {
  def execute(context: Context) = {
    if (testFunction.apply(context))
      nextTrue.execute(context)
    else
      nextFalse.getOrElse(nextAfter).execute(context)
  }
}