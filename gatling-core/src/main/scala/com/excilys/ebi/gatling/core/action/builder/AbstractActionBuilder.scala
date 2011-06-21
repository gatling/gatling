package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.AbstractAction

abstract class TRUE
abstract class FALSE

trait AbstractActionBuilder {
  def build: AbstractAction
  def withNext(next: AbstractActionBuilder): AbstractActionBuilder
}