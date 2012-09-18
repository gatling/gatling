package com.excilys.ebi.gatling.core.structure

import com.excilys.ebi.gatling.core.action.builder.BypassSimpleActionBuilder
import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.session.Session

trait Feeds[B] extends Execs[B] {

	/**
	 * Method used to load data from a feeder in the current scenario
	 *
	 * @param feeder the feeder from which the values will be loaded
	 */
	def feed(feeder: Feeder): B = {

		val feedFunction = (session: Session) => {
			if (!feeder.hasNext) {
				error("Feeder is now empty, stopping engine")
				system.shutdown
				sys.exit
			}

			session.setAttributes(feeder.next)
		}
		newInstance(BypassSimpleActionBuilder(feedFunction) :: actionBuilders)
	}
}