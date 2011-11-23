package com.excilys.ebi.gatling.core.check

trait CheckBuilderFind[B <: CheckBuilder[B, _]] extends CheckBuilderVerify[B] { this: CheckBuilder[B, _] with CheckBuilderVerify[B] =>
	private def find(occurrence: Int) = newInstanceWithFind(occurrence)

	def first = find(0)
}