package com.excilys.ebi.gatling.core.util

object PathHelper {
  val GATLING_HOME = System.getenv("GATLING_HOME")

  val GATLING_CONFIG_FOLDER = GATLING_HOME + "/conf"
  val GATLING_RESULTS_FOLDER = GATLING_HOME + "/results"
  val GATLING_USER_FILES_FOLDER = GATLING_HOME + "/user-files"
  val GATLING_ASSETS_FOLDER = GATLING_HOME + "/assets"

  val GATLING_JS_JQUERY = "/js/jquery.min.js"
  val GATLING_JS_HIGHCHARTS = "/js/highcharts.js"
  val GATLING_RAWDATA_FOLDER = "/rawdata"
  val GATLING_SIMULATION_LOG_FILE = "simulation.log"
  val GATLING_CONFIG_FILE = "gatling.conf"

  val GATLING_GRAPH_ACTIVE_SESSIONS_FILE = "active_sessions.html"
  val GATLING_GRAPH_GLOBAL_REQUESTS_FILE = "requests.html"
  val GATLING_STATS_GLOBAL_REQUESTS_FILE = "requests.tsv"

  val GATLING_TEMPLATE_REQUEST_DETAILS_BODY_FILE = "templates/details_requests_body.ssp"
  val GATLING_TEMPLATE_HIGHCHARTS_COLUMN_FILE = "templates/highcharts_column.ssp"
  val GATLING_TEMPLATE_HIGHCHARTS_TIME_FILE = "templates/highcharts_time.ssp"
  val GATLING_TEMPLATE_LAYOUT_FILE = "templates/layout.ssp"

  val GATLING_SEEDS_FOLDER = GATLING_USER_FILES_FOLDER + "/seeds"
  val GATLING_SCENARIOS_FOLDER = GATLING_USER_FILES_FOLDER + "/scenarios"
  val GATLING_REQUEST_BODIES_FOLDER = GATLING_USER_FILES_FOLDER + "/request-bodies"
  val GATLING_TEMPLATES_FOLDER = GATLING_USER_FILES_FOLDER + "/templates"

  val GATLING_ASSETS_JQUERY = GATLING_ASSETS_FOLDER + GATLING_JS_JQUERY
  val GATLING_ASSETS_HIGHCHARTS = GATLING_ASSETS_FOLDER + GATLING_JS_HIGHCHARTS
}