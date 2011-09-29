package com.excilys.ebi.gatling.core.action
import com.excilys.ebi.gatling.core.context.Context

class WhileAction(testFunction: Context => Boolean, var nextTrue: WhileAction => Action, nextAfter: Action) extends Action {

  val nextTrueAction = nextTrue.apply(this)

  def execute(context: Context) = {

    if (testFunction.apply(context)) {
      nextTrueAction.execute(context)
    } else {
      context.resetWhileDuration
      nextAfter.execute(context)
    }
  }
}