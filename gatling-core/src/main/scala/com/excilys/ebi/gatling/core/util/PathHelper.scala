package com.excilys.ebi.gatling.core.util
import scala.tools.nsc.io.Path

object PathHelper {

	implicit def path2jfile(path: Path) = path.jfile

	implicit def path2string(path: Path) = path.toString
}