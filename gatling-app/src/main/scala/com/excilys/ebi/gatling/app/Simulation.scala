package com.excilys.ebi.gatling.app
import com.excilys.ebi.gatling.core.scenario.configuration.ScenarioConfigurationBuilder

trait Simulation extends Function0[Seq[ScenarioConfigurationBuilder]]
