package com.excilys.ebi.gatling.core.init
import java.util.concurrent.atomic.AtomicBoolean

trait Initializable {

	protected var initialized = new AtomicBoolean(false)
}