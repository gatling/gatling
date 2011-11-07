package com.excilys.ebi.gatling.core.util

trait ClassSimpleNameToString {

	override def toString = this.getClass().getSimpleName()
}