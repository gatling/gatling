package com.excilys.ebi.gatling.core.action.builder
import com.excilys.ebi.gatling.core.action.Action

object GroupActionBuilder {
  class GroupActionBuilder(groupName: Option[String], end: Option[Boolean]) extends AbstractActionBuilder {

    def isEnd = end.get

    def getName = groupName.get

    def build(scenarioId: Int) = throw new UnsupportedOperationException

    def withNext(next: Action) = throw new UnsupportedOperationException

    def inGroups(groups: List[String]) = throw new UnsupportedOperationException
  }

  def startGroupBuilder(groupName: String) = new GroupActionBuilder(Some(groupName), Some(true))
  def endGroupBuilder(groupName: String) = new GroupActionBuilder(Some(groupName), Some(false))
}