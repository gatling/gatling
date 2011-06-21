package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.Action

abstract class TRUE
abstract class FALSE

trait AbstractActionBuilder {
  def build: Action
  def withNext(next: AbstractActionBuilder): AbstractActionBuilder
}