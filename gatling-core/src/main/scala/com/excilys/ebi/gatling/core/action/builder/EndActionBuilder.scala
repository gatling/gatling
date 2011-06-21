package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.AbstractAction
import com.excilys.ebi.gatling.core.action.EndAction

object EndActionBuilder {
  class EndActionBuilder extends AbstractActionBuilder {

    def build(): AbstractAction = new EndAction

    def withNext(next: AbstractActionBuilder): AbstractActionBuilder = null
  }

  def endActionBuilder = new EndActionBuilder
}