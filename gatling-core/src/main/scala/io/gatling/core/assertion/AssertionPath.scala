package io.gatling.core.assertion

object AssertionPath {
  implicit def string2assertionPath(path: String) = AssertionPath(List(path))
}

case class AssertionPath(parts: List[String]) {

  def /(part: String) = copy(parts = parts :+ part)

}
