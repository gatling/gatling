package com.excilys.ebi.gatling.statistics.presenter

abstract class DataPresenter[R] {
	def generateGraphFor(runOn: String, results: R)
}