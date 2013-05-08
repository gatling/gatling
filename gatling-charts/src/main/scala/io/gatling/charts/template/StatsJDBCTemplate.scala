/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.charts.template

import com.dongxiguo.fastring.Fastring.Implicits._
import io.gatling.charts.component.{ GroupedCount, RequestStatistics, Statistics }
import io.gatling.charts.component.Statistics.PrintableStat
import io.gatling.core.result.writer.ConsoleSummary.{ newBlock, outputLength, writeSubTitle }
import io.gatling.core.util.StringHelper.{ eol, RichString }
import java.sql.{ Connection, Statement, DriverManager, ResultSet, PreparedStatement}
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.writer.JDBCDataWriter.ExecuteAndClearBatch
import io.gatling.charts.report.Container.{ GROUP, REQUEST }
import io.gatling.core.result.reader.DataReader
import io.gatling.core.result.StatsPath
import io.gatling.core.result.GroupStatsPath
import io.gatling.core.result.RequestStatsPath
import io.gatling.core.result.reader.GeneralStats
import io.gatling.core.result.Group

class StatsJDBCTemplate(requestStatistics: RequestStatistics, runId : String, dataReader: DataReader) {
  
	private var conn: Connection = _
	private var statement: Statement = _
	private var requestStatement: PreparedStatement = _
	private var groupStatement: PreparedStatement = _
	private var detailsStatement: PreparedStatement = _
  
	def initialize = { 
		conn = DriverManager.getConnection(configuration.data.jdbc.db.url, configuration.data.jdbc.db.username, configuration.data.jdbc.db.password)
		conn.setAutoCommit(false)
		statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS `GlobalStats` ( `runId` VARCHAR(450) NOT NULL , `name` VARCHAR(450) NULL , `total` bigint(20) NULL , `ok` bigint(20) NULL , `ko` bigint(20) NULL );")
		statement.executeUpdate("CREATE TABLE IF NOT EXISTS `GroupStats` (`runId` VARCHAR(450) NOT NULL , `name` VARCHAR(450) NULL , `count` INT NULL , `percentage` INT NULL)")
		statement.executeUpdate("CREATE  TABLE IF NOT EXISTS `DetailsStats` ( `runId` VARCHAR(450) NOT NULL , `request` VARCHAR(450) NULL,  `min` INT NULL , `max` INT NULL , `count` INT NULL , `mean` INT NULL , `stdDev` INT NULL , `percentil95` INT NULL , `percentile99` INT NULL , `meanRequestsPerSec` INT NULL );")
		requestStatement = conn.prepareStatement("INSERT INTO `GlobalStats`(`runId`, `name`, `total`, `ok`, `ko`) VALUES (?,?,?,?,?)")
		groupStatement = conn.prepareStatement("INSERT INTO `GroupStats`(`runId`, `name`, `count`, `percentage`) VALUES (?,?,?,?)")
		detailsStatement = conn.prepareStatement("INSERT INTO `DetailsStats` (`runId`, `request`, `min`, `max`, `count`, `mean`, `stdDev`, `percentil95`, `percentile99`, `meanRequestsPerSec`) VALUES (?,?,?,?,?,?,?,?,?,?);")

	}
	
	def addRequestStats(statistics: Statistics) = {
		import statistics._
		requestStatement.setString(1, runId)
		requestStatement.setString(2, name)
		requestStatement.setLong(3, total)
		requestStatement.setLong(4, success)
		requestStatement.setLong(5, failure)
		requestStatement.addBatch
	}
	
	def addGroupedStats(groupedCount: GroupedCount) = {
		import groupedCount._
		groupStatement.setString(1, runId)
		groupStatement.setString(2, name)
		groupStatement.setInt(3, count)
		groupStatement.setInt(4, percentage)
		groupStatement.addBatch
	}
	
	def addDetailsRequestStats(request: String, group: Option[Group])= {
	  val generalStats = dataReader.generalStats(None, Some(request), group)
	  import generalStats._
	  detailsStatement.setString(1, runId)
	  detailsStatement.setString(2, request)
	  detailsStatement.setInt(3, min)
	  detailsStatement.setInt(4, max)
	  detailsStatement.setInt(5, count)
	  detailsStatement.setInt(6, mean)
	  detailsStatement.setInt(7, stdDev)
	  detailsStatement.setInt(8, percentile1)
	  detailsStatement.setInt(9, percentile2)
	  detailsStatement.setInt(10, meanRequestsPerSec)
	  detailsStatement.addBatch
	}

	
	def writeToDatabase = {
	  if(configuration.data.jdbc.stats || configuration.data.jdbc.simulation) 
	    {
	    initialize
		    try{
				import requestStatistics._
				if(configuration.data.jdbc.stats){
					//Requests Stats
					addRequestStats(numberOfRequestsStatistics)
					addRequestStats(minResponseTimeStatistics)
					addRequestStats(maxResponseTimeStatistics)
					addRequestStats(meanStatistics)
					addRequestStats(stdDeviationStatistics)
					addRequestStats(percentiles1)
					addRequestStats(percentiles2)
					requestStatement.executeAndClearBatch
					//Grouped Stats
					groupedCounts.map(addGroupedStats)
					groupStatement.executeAndClearBatch
				}
				if(configuration.data.jdbc.simulation){
					dataReader.statsPaths.foreach {
						case RequestStatsPath(request, group) => addDetailsRequestStats(request, group)
						case GroupStatsPath(group) => 
					}
					detailsStatement.executeAndClearBatch
				}
		  	} finally{
		  		conn.close
		  	}
	    }
	}
}
