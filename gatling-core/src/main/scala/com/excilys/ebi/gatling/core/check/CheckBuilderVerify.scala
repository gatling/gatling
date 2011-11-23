package com.excilys.ebi.gatling.core.check
import com.excilys.ebi.gatling.core.check.strategy.CheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.ExistenceCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.NonExistenceCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.EqualityCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.NonEqualityCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.InRangeCheckStrategy
import com.excilys.ebi.gatling.core.check.strategy.InRangeCheckStrategy.rangeToString

trait CheckBuilderVerify[B <: CheckBuilder[B, _]] extends CheckBuilderSave[B] { this: CheckBuilder[B, _] with CheckBuilderSave[B] =>
	def verify(strategy: CheckStrategy) = newInstanceWithVerify(strategy)

	def verify(strategy: CheckStrategy, expected: String) = newInstanceWithVerify(strategy, Some(expected))

	def exists = verify(ExistenceCheckStrategy)

	def notExists = verify(NonExistenceCheckStrategy)

	def eq(expected: String) = verify(EqualityCheckStrategy, expected)

	def neq(expected: String) = verify(NonEqualityCheckStrategy, expected)

	def in(range: Range) = verify(InRangeCheckStrategy, range)
}