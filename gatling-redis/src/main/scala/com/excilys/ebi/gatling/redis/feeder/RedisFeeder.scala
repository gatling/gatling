package com.excilys.ebi.gatling.redis.feeder

import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.redis.RedisClientPool

import grizzled.slf4j.Logging

object RedisFeeder extends Logging {

	def apply(clientPool: RedisClientPool, key: String): Feeder = {

		system.registerOnTermination(clientPool.close)

		new Iterator[Map[String, String]] {

			def next = {
				clientPool.withClient {
					client =>
						val value = client.lpop(key).getOrElse {
							error("There are not enough records in the feeder '" + key + "'.\nPlease add records or use another feeder strategy.\nStopping simulation here...")
							clientPool.pool.close
							system.shutdown
							sys.exit
						}
						Map(key -> value)
				}
			}

			def hasNext = true
		}
	}
}