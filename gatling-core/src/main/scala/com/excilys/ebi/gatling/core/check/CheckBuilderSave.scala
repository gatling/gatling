package com.excilys.ebi.gatling.core.check

trait CheckBuilderSave[B <: CheckBuilder[B, _]] { this: CheckBuilder[B, _] =>
	def saveAs(attrName: String) = newInstanceWithSaveAs(attrName)
}