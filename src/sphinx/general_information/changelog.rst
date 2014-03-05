#########
Changelog
#########

:version:`v2.0.0-M3a <43>`
==========================

.. note:: This version fixes several major bugs in 2.0.0-M3, users are recommended to upgrade (transparent from M3)

Fixes
-----

* :issue:`1281` Core: XPath check fails, ByteBufferInputStream buffer overflow
* :issue:`1282` Core: Simulation doesn't terminate properly
* :issue:`1283` HTTP: Wrong time out
* :issue:`1286` HTTP: Header regex check fails with NPE

:version:`v2.0.0-M3 <36>`
=========================

This version also contains all the fixes and features from 1.5.1 and 1.5.2.

.. note:: Beware of the breaking changes since 2.0.0-M2, like protocol set up and HTTP request bodies, see Gatling 2 page.

Fixes
-----

* :issue:`1140` Core: Fix during loop
* :issue:`1148` Core: Remove Expression type alias that messes up with implicit conversions
* :issue:`1153` Recorder: Bug with URI parsing
* :issue:`1154` Recorder: Protect headers
* :issue:`1183` Charts: Incorrect OK/KO counts on groups
* :issue:`1207` HTTP: Additional CRLF when no Content-Disposition in multipart
* :issue:`1219` Recorder: Protected values in HAR files
* :issue:`1224` Core: Don't eagerly fetch resources from classpath
* :issue:`1243` Core: Div by 0 in ramp injection
* :issue:`1244` Charts: Percentage round up
* :issue:`1257` Core: TryMax exit condition
* :issue:`1259` Core: Wrong behavior when existASAP = false (repeat, foreach)
* :issue:`1266` Core: TryMax doesn't log inner groups
* :issue:`1270` App: Wrong behavior when simulation compiled class already exists

Features
--------

* :issue:`62` and :issue:`1142` JDBC: Store stats in RDBMS, contributed by Jay Patel (@jaypatel512)
* :issue:`1141` Core: Simulation set up refactoring
* :issue:`1083`, :issue:`1084` and :issue:`1208`  Core: Support loading resources (feeder, bodies) from classpath
* :issue:`1163` and :issue:`1166` HTTP: Multipart support
* :issue:`1176` Recorder: Generate inline queries instead of queryParams
* :issue:`1181` Charts: Display KO percentages
* :issue:`1182` and :issue:`1265` Recorder: Handle pauses around tags, contributed by Sébastien Keller (@Skeebl)
* :issue:`1185` Core: Expose XML parser config in gatling.conf
* :issue:`1211` HTTP: Drop Scalate's SSP
* :issue:`1226` HTTP: Make SSL store type optional
* :issue:`1237` HTTP: Make request bodies cache configurable
* :issue:`1252` Charts: Add errors distribution table
* :issue:`1269` HTTP: Support Grizzly as HTTP provider

:version:`v1.5.2 <41>`
======================

Fixes
-----

* :issue:`1203` Recorder: Recorder doesn't record binary bodies correctly
* :issue:`1204` Core: CSV parser escapeChar doesn't work properly
* :issue:`1213` Maven: When skipping, maven plugin shouldn't parse args
* :issue:`1214` Core: Race condition in DataWriter.uninitialized
* :issue:`1216` HTTP: Have query param support both "foo=" and "foo" forms
* :issue:`1223` Core: JsonPath with array element NPE when the array is actually null
* :issue:`1225` Charts: Dots in javascript variable names make jQuery crash

Features
--------

* :issue:`1209` HTTP: Gatling doesn't honor RFC6265 cookie encoding style
* :issue:`1217` HTTP: Support AHC maxConnectionLifeTime
* :issue:`1221` HTTP: HEAD verb can have a body
* :issue:`1222` HTTP: Add OPTIONS verb support
* :issue:`1235` HTTP: Expose Virtual Host configuration
* :issue:`1240` HTTP: Add HttpProtocol.baseHeaders(headers)

:version:`v1.5.1 <40>`
======================

.. note ::
	From this version, one can:
	
	* use ``-Dgatling.test.skip`` in order to disable the gatling maven plugin
	* chose between ``jodd`` and ``jsoup`` in ``gatling.conf`` for the CSS Selector engine
	* display multiple Simulations (launched in multiple maven plugin executions for example) in the Jenkins plugin

Fixes
-----

* :issue:`1139` Core: Round-Robin feeder memory leak
* :issue:`1146` HTTP: Set AHC maxRetry to 0 by default
* :issue:`1155` Recorder: Recorder doesn't dump with selected encoding
* :issue:`1157` Recorder: Recorder doesn't properly extract bodies

Features
--------

* :issue:`1063` Maven: Allow one to skip gatling tests
* :issue:`1138` HTTP: Backport support for poorly encoded Location header
* :issue:`1145` Core: Let exec take a Scenario
* :issue:`1160` HTTP: Add session to log triggered by request failure
* :issue:`1162` Jenkins: Multiple gatling runs in single jenkins job, thanks to @cprice-puppet
* :issue:`1170` Core: Revive Jodd support, make it default implementation
* :issue:`1171` Core: Backport extractors caching to 1.5

:version:`v2.0.0-M2 <35>`
=========================

This version also contains all the fixes and features from 1.5.0.

Fixes
-----

* :issue:`1093` Charts: Broken link if request name contains 
* :issue:`1098` App: Fix regex for scala-compiler jar, thanks to @nap-stig
* :issue:`1124` Charts: Invalid JSON file
* :issue:`1136` Core: else branch of ifOrElse always ignored

Features
--------

* :issue:`827` and :issue:`1085` Recorder: Generate Simulation from HAR file
* :issue:`966` and :issue:`1090` Output final stats to console, thanks to @jaypatel512
* :issue:`1091` Core: ``dirac`` renamed into ``heaviside``
* :issue:`1105` and :issue:`1113` Add an ``exitASAP`` to ``during`` and ``asLongAs`` loops so that exit condition is evaluated on every action instead of once per iteration

:version:`v1.5.0 <38>`
======================

Fixes
-----

* :issue:`1094` HTTP: JsonPath and XPath checks consume the response body
* :issue:`1095` Charts: Charts names encoding problem
* :issue:`1116` Core: DataWriter race condition
* :issue:`1127` Recorder: Invalid dot in chains
* :issue:`1133` HTTP: Don't drop query params inlined in the URL

Features
--------

* :issue:`1055` Core: Reorganize gatling.conf
* :issue:`1099` Core: Override ``gatling.data.writers`` from System properties
* :issue:`1101` and :issue:`1102` Recorder: Override default keystore, thanks to @cprice-puppet
* :issue:`1114` Core: Switch from Jodd to Jsoup as CSS selectors implementation
* :issue:`1115` Drop custom JsonPath in favor of Jayway one
* :issue:`1117` HTTP: Add ``HttpProtocolConfiguration.shareClient`` to have one http client per user (disabled by default)
* :issue:`1119` HTTP: Add ``HttpProtocolConfiguration.shareConnections`` to have one http client per user (enabled by default)
* :issue:`1121` HTTP: Add Session attributes for setting SSL Engine per user
* :issue:`1125` Core: Add a light mode for ConsoleDataWriter
* :issue:`1126` Graphite: Add a light mode for GraphiteDataWriter
* :issue:`1134` and :issue:`1135` App: Allow simulation description to be configurable via conf file / CLI, thanks to @cprice-puppet
* :issue:`1137` Refactor doIfOrElse(String, String) into doIfEqualsOfElse(Expression[String], Expression[String])

.. note:: Beware that the configuration file has changed, so the System property names to override it too! Please check the new file.

:version:`v2.0.0-M1 <23>`
=========================

See full :version:`milestone content <23>`.

:version:`v1.4.7 <37&>`
=======================

Fixes
-----

* :issue:`1047` and :issue:`1049` Maven: Forked JVM might not exit on Windows, thanks to alvinlin123!
* :issue:`1066` Maven: System properties propagation still not working under Windows
* :issue:`1074` Charts: Broken records cause generation to fail
* :issue:`1080` Charts: Javascript error when request name starts with a number

Features
--------

* :issue:`1050` Core: Upgrade Slf4j 1.7.5
* :issue:`1051` Core: Upgrade Logback 1.0.11
* :issue:`1070` Charts: Long request names break the menu
* :issue:`1072` HTTP: Upgrade AsyncHttpClient 1.7.13
* :issue:`1073` HTTP: Upgrade Netty 3.6.5

:version:`v1.4.6 <34>`
======================

Fixes
-----

* :issue:`1018` and :issue:`1019` Core: Possible race condition on initialization
* :issue:`1020` HTTP: Warm up done twice, slowing down start up
* :issue:`1037` Maven: Renaming property ``simulation`` into ``simulationClass`` in order to avoid clash when passing as System property
* :issue:`1042` Recorder: Invalid generated simulation when first element is a tag
* :issue:`1044` Maven: Fails when propagating a System property with a space

Features
--------

* :issue:`1046` Core: Drop attribute axis support in JsonPath, introduce magic value ``_`` to target root array

.. note ::
	Be aware of the 2 breaking changes:
	
	* Maven plugin property ``simulation`` renamed into ``simulationClass``
	* JsonPath not longer supports attribute axis (didn't really make sense and was equivalent to child element one).

:version:`v1.4.5 <33>`
======================

Fixes
-----

* :issue:`995` and :issue:`1013` Core: Can't use assertions with "manual scaling" procedure, see :ref:`scaling-out`
* :issue:`1003` Maven: line.separator disappears when propagation System properties
* :issue:`1009` Core: config doesn't get overridden with System properties

Features
--------

* :issue:`1017` HTTP: Upgrade to Async Http Client 1.7.12

:version:`v1.4.4 <32>`
======================

Fixes
-----

* :issue:`971` Maven: Plugin broken, wrong Scala version resolved
* :issue:`974` HTTP: XPath and JsonPath checks crash on empty body
* :issue:`984` and :issue:`993` Charts: stats.tsv file broken

Features
--------

* :issue:`906`, :issue:`911` and :issue:`972` Core: add new ``foreach`` DSL, see :ref:`doc <foreach>`
* :issue:`960` Core: Make user ids unique
* :issue:`977` Maven: Propagate System properties in fork mode
* :issue:`983` Charts: Make Graphite root configurable, defaulting to gatling
* :issue:`996` HTTP: Make SslContext configurable

+ tons of optimizations, see full issues list for details

:version:`v1.4.3 <31>`
======================

Fixes
-----

* :issue:`914`, :issue:`915`, :issue:`918`, :issue:`919` Charts: Charts not rendering when request/group name contains special characters
* :issue:`922` Core: Fix pause shift computation
* :issue:`926` HTTP: Improve domain matching in cookie handling
* :issue:`942` Recorder: Handle poorly encoded queries
* :issue:`944` Core: NPE when running a simulation with 2 scenarios with the same name
* :issue:`954` and :issue:`956` HTTP: Filtered out multiple cookies with the same name/path (support PHP bug)

Features
--------

* :issue:`913` HTTP: Upgrade Netty 3.6.2
* :issue:`934` HTTP: Upgrade async-http-client 2.10
* :issue:`941` Core: Loops clean up
* :issue:`957` HTTP: Add Connection to possible common headers

:version:`v1.4.2 <30>`
======================

Fixes
-----

* :issue:`881` and :issue:`910` Core: Fix Zinc incremental compiler NPE
* :issue:`898` Charts: invalid group OK/KO stats
* :issue:`899` HTTP: Cookies are not propagated from HTTP to HTTPS
* :issue:`907` JDBC: JdbcFeeder SQLException with Oracle

:version:`v1.4.1 <29>`
======================

Fixes
-----

* :issue:`882` Core: Possible SOE when too many requests
* :issue:`884` Recorder: Not working with HTTPS
* :issue:`886` Charts: Wrong statistics for groups
* :issue:`893` Maven: reportsOnly not work with maven plugin

Features
--------

* :issue:`889` Core: Upgrade Scala 2.9.3-RC1
* :issue:`892` Core: Upgrade Akka 2.0.5
* :issue:`894` HTTP: Upgrade Netty 3.6.1

:version:`v1.4.0 <22>`
======================

Fixes
-----

* :issue:`844` Charts: Handle parentheses in request names
* :issue:`846` Core: Fix possible NPE in FileDataWriter.sanitize
* :issue:`866` :issue:`867` Charts: Fix stats.tsv file header, thanks to @caps (Pete Capra)

Features
--------

* :issue:`170` :issue:`322` Charts: New API for grouping requests
* :issue:`560` Core: New API for acceptance criteria
* :issue:`594` Maven: New Jenkins plugin
* :issue:`772` Charts: Reorganize description
* :issue:`782` Core: 1.3.X deprecated APIs removed
* :issue:`788` :issue:`810` Core: Jackson fully configurable through gatling.conf
* :issue:`802` HTTP: Host header doesn't have to be specified as AHC computes it
* :issue:`829` Core: Feeder is now Iterator[Map[String, T]]
* :issue:`832` Charts: Upgrade jQuery 1.8.3
* :issue:`838` :issue:`840` Core: Breaking change in Simulation structure: remove apply and configure, introduce setUp
* :issue:`839` Maven: maven plugin now use src/test/scala and src/test/resources folders
* :issue:`841` Core: New ``.size`` EL function
* :issue:`847` Core: Make extraResponseInfoExtractor take an ExtendedResponse
* :issue:`848` :issue:`879` HTTP: Better tracing/debugging of requests and responses
* :issue:`849` HTTP: Upgrade Netty 3.6.0.Final
* :issue:`857` Core: Stop engine nicely instead of System.exit on feeder starvation
* :issue:`860` Core: Upgrade Logback 1.0.9
* :issue:`861` Core: Upgrade Jackson 2.1.2
* :issue:`864` :issue:`872` Maven: maven-gatling-plugin refactoring
* :issue:`870` HTTP: Make fileBody dynamic
* :issue:`874` Core: Fix Zinc when Gatling path contains special characters
* :issue:`876` App: Rename deb package name into gatling-tool
* :issue:`877` HTTP: Upgrade AHC 1.7.9, fix bug when no headers
* :issue:`880` Charts: Upgrade Highcharts 2.3.5 and Highstock 1.2.5

.. warning:: This release introduce a breaking change in the Simulation classes format. Deprecated methods in 1.3.X have been removed. See :ref:`migration guide <1.4.X-migration>`

:version:`v1.3.5 <28>`
======================

Fixes
-----

* :issue:`799` Maven: src/test/scala directory missing in projects generated with the archetype
* :issue:`800` Core: Fix debig logger in logback.xml
* :issue:`808` HTTP: Original ContentType header shouldn't be propagated when redirecting
* :issue:`813` HTTP: followRedirect not working properly when Location contains a query
* :issue:`816` HTTP: CookieStore not accounting for port number in domain computation
* :issue:`820` Core: JsonPath not supporting wildcards

Features
--------

* :issue:`765` and :issue:`814` HTTP: Print HTTP params in debug
* :issue:`792` Core: Make request names dynamic
* :issue:`817` HTTP: Authorization header can now be declared as a common header in HttpProtocolConfiguration
* :issue:`818` HTTP: Support for non UTF-8 encoded cookies (value containing an "=" character)

:version:`v1.3.4 <27>`

Fixes
-----

* :issue:`785` Recorder: Fix followRedirect support
* :issue:`786` Core: Fix during loop timer
* :issue:`787` Core: Fix JsonPath

:version:`v1.3.3 <26>`
======================

Features
--------

* :issue:`754` Core: Use Zinc incremental compiler
* :issue:`763` and :issue:`769` Charts: Redesign statistic summary table
* :issue:`775` HTTP/Recorder: DNT common header support
* :issue:`779` Core: Loop index is now directly exposed as Session attribute

Fixes
-----

* :issue:`755` Charts: Square and curly braces in request names mess up with javascript
* :issue:`756` HTTP: Fix abusing caching
* :issue:`759` Core: Fix check when forcing Simulation
* :issue:`760` Maven: add missing logback.xml file
* :issue:`762` HTTP: Support Expires headers numerical values
* :issue:`766` Metrics: Max and count should be reseted along with the buckets
* :issue:`770` Charts: Run description is not properly printed
* :issue:`777` Core: Fix unrecoverable crash on while condition evaluation
* :issue:`778` Core: Counter should be incremented before the loop content
* :issue:`780` Recorder: Fix pauses shift

:version:`v1.3.2 <25>`
======================

Features
--------

* :issue:`750` HTTP: better support of multivalued params and queryParams with multiValuedParam and multiValueQueryParam, see :ref:`doc <http-query-parameters>`

Fixes
-----

* :issue:`753` HTTP: regression: params were being sent as query params

:version:`v1.3.1 <24>`
======================

Features
--------

* :issue:`743` Checks: Add new bodyString check, see :ref:`doc <checks-response-body>`
* :issue:`744` HTTP: Upgrade Netty to 3.5.8.Final, critical performance fixes
* :issue:`752` Config: Add aliases for built-ins data readers and writers: console, file and graphite

Fixes
-----

* :issue:`732` HTTP: responseChunksDiscardingEnabled was not working properly
* :issue:`734` HTTP: Host header was being ignored, fixed thanks to @dustinbarnes
* :issue:`735` Core: NPE when using chain
* :issue:`736` Charts: Drop Scalding/Cascading, considerably reduce memory footprint, introduce accuracy config parameter defaulting to 10ms
* :issue:`745` Recorder: Tags were not dumped in the generated Simulation
* :issue:`747` Charts: Some charts disappear. This is due to a Highstock bug that has been fixed yet, so a workaround was introduced
* :issue:`751` Feeders and Charts: File streams were not properly closed

.. note:: Due to the new accuracy parameter, you will experience less precise values in the charts. You can get the old behavior by setting a 1 ms value, at the cost of a higher memory usage.

:version:`v1.3.0 <20>`
======================

.. warning:: Migration required, see :ref:`migration guide <1.3.0-migration>`

API changes:

* :issue:`669` Core: Config file format change
* :issue:`698` Core: Durations expressed as (value, unit) are deprecated
* :issue:`699` Core: Loops refactoring, old ones are deprecated
* :issue:`705` Core: insertChain deprecated, use exec
* :issue:`711` Core: Feeders are now Iterators
* :issue:`730` Core: doIf refactoring

Features
--------

* :issue:`592` Charts: Display mean number of requests/sec in global chart page
* :issue:`593` Charts: Generate a csv file with global stats
* :issue:`604` and :issue:`672` Charts: Response time and latency charts now display (min, max) ranges instead of a max values line
* :issue:`606` HTTP: New byteArrayBody(Session => Array[Byte]), see :ref:`doc <http-byte-array-body>`
* :issue:`607` HTTP: New baseUrls for round-robin, thanks to @israel, see :ref:`doc <http-base-url>`
* :issue:`607` and :issue:`683` Charts: New summary table on global page
* :issue:`621` Checks: Css checks underlying parser now supports browser conditional tests
* :issue:`623` HTTP: New caching support, see :ref:`doc <http-caching>`
* :issue:`624` Core: New console dashboard
* :issue:`627` Checks: New currentLocation check, see :ref:`doc <checks-current-location>`
* :issue:`628` Core: New pauseCustom(() => Long), see :ref:`doc <custom-pause>`
* :issue:`641` and :issue:`658` HTTP: Log requests and responses on failure
* :issue:`644` HTTP: paramKey and fileName upload parameters are now dynamic
* :issue:`646` HTTP: Multi file upload support, thanks to @codemnky
* :issue:`647` and :issue:`690` Core: New randomSwitch, see :ref:`doc <random-switch>`
* :issue:`652` HTTP: New disableResponseChunksDiscarding, see :ref:`doc <dumping-custom-data>`
* :issue:`652` Checks: Css checks now support attribute node selection, see :ref:`doc <checks-css>`
* :issue:`674` and :issue:`675` Graphite: Gatling can now report to Graphite, see :ref:`corresponding page <graphite>`
* :issue:`685` Project: Continuous Integration now on Cloudbees
* :issue:`688` Charts: New polar chart with request counts
* :issue:`701` Core: New exitBlockOnFail and exitHereIfFailed, see doc : :ref:`exitBlockOnFail <exit-block-on-fail>` & :ref:`exitHereIfFailed <exit-here-if-failed>`
* :issue:`702` Core: New tryMax, see :ref:`doc <tryMax>`
* :issue:`703` Core: Remove bootstrapping from chain, see :ref:`doc <bootstrap>`
* :issue:`706` Core: new randomRoundRobin, see :ref:`doc <round-robin-switch>`
* :issue:`712` Core: Let exec take a chain vararg, see :ref:`doc <exec>`
* :issue:`714` Core: Better simulations compilation warnings

Fixes
-----

* :issue:`571` HTTP: Better cookies support
* :issue:`609` HTTP: NPE when Location header missing
* :issue:`615` HTTP: Url is encoded twice on redirect
* :issue:`630` Charts: Fix percentiles
* :issue:`639` and :issue:`687` Recorder: should ask before overwriting
* :issue:`651` Check: responseTime and latency checks mustn't cause body to be stored
* :issue:`653` HTTP: Duration computation problems
* :issue:`664` Core: Don't display Abstract simulations
* :issue:`665` Core: LinkageError when using inheritance
* :issue:`709` Recorder: support empty valued parameters
* :issue:`713` and :issue:`715` Charts: support quotes in request names


:version:`v1.2.5 <21>`
======================

Features
--------

* :issue:`596` Better live informations

Fixes
-----

* :issue:`597` Fix cookie handling regression
* :issue:`599` Time measurement is intrinsically imprecise, ensure that it can't cause negative response times
* :issue:`600` Fix response time distribution chart, max value wasn't properly displayed
* :issue:`601` Fix gatling-maven-plugin JVM arguments

:version:`v1.2.4 <16>`
======================

Features
--------

* :issue:`446` Add the ability to dump custom data in the logs, thanks to Stephen Kuenzli, see :ref:`doc <dumping-custom-data>`
* :issue:`569` New reponseTimeInMillis and latencyInMillis checks, see :ref:`doc <check-response-time>`
* :issue:`576` new headerRegex check, see :ref:`doc <check-headerRegex>`
* :issue:`591` Location header is now automatically decoded when checked, see :ref:`doc <checks-header>`
* :issue:`595` New simple feeder

Fixes
-----

* :issue:`572` Fix a bug where cookies with the same name could be sent both under certain conditions
* :issue:`573` Fix script variables scope under Windows, thanks to Henri Tremblay
* :issue:`574` Fix logger in logback.conf, thanks to Henri Tremblay
* :issue:`583` Fix engine encoding handling
* :issue:`586` Fix recorder class name and package generation
* :issue:`587` Fix recorder encoding handling

:version:`v1.2.3 <15>`
======================

Fixes
-----

* :issue:`566` Fix body checks regression in 1.2.2

:version:`v1.2.2 <14>`
======================

.. warning:: due to :issue:`566`, 1.2.2 users are recommended to upgrade to 1.2.3

Features
--------

* :issue:`543` Make charts generation consume multiple simulation(.*).log files, ease multiple instances testing, see :ref:`doc <scaling-out>`
* :issue:`548` New `Redis <http://redis.io>`_ Feeder, thanks to @krishnenc (Krishnen Chedambarum), see :ref:`doc <redis>`
* :issue:`548` New byteArrayBody, thanks to @krishnenc (Krishnen Chedambarum), see :ref:`doc <http-byte-array-body>`
* :issue:`552` Gatling modules can now be built independantly, thanks to @nire (Nicolas Rémond)
* :issue:`553` New checksum checks, see :ref:`doc <checks-checksum>`
* :issue:`555` Run name can now be set on gatling-maven-plugin, see :ref:`doc <maven-advanced-configuration>`
* :issue:`557` Gatling now ships `Grizzly <http://grizzly.java.net>`_ to ease switching NIO provider (Gatling still uses Netty by default)

Fixes
-----

* :issue:`562` Fix gatling-maven-plugin crash when setting no-reports or run-name options, thanks to @skuenzli (Stephen Kuenzli)
* :issue:`558` Ensure IfAction and WhileAction don't lose messages on crash

:version:`v1.2.1 <13>`
======================

.. warning:: Due to :issue:`545`, 1.2.0 users are highly recommended to upgrade!

Features
--------

* :issue:`539` much better reports generation memory footprint

Fixes
-----

* :issue:`536` HttpProtocolConfiguration DSL wouldn't compile when proxy was defined in the middle of the chain
* :issue:`537` Warm up request would break the run when target url cannot be reached
* :issue:`538` Fix scatter plot chart
* :issue:`540` Fix percentile ordinal suffix, thanks to Nicolas Rémond
* :issue:`544` Fix times computation at very high throughput
* :issue:`545` Fix pause duration
* :issue:`546` Fix launch script when path contains special characters, thanks to Jean-François Bilger

:version:`v1.2.0 <6>`
======================

.. warning:: Migration required, see :ref:`migration guide <1.2.0-migration>`

Features
--------

* :issue:`376` loop times condition can now be dynamic
* :issue:`432` & :issue:`523` Referer header can now be automatically computed
* :issue:`435` & :issue:`518` CSS Selector extractors are now supported, thanks to Nicolas Rémond (@nire)
* :issue:`493` & :issue:`531` HEAD HTTP word is now supported, thanks to Nicolas Rémond (@nire)
* :issue:`501` Reports global page has been refactored
* :issue:`509` Recorder has been migrated to Scala
* :issue:`514` Common HTTP Headers can be configured on ProtocolConfiguration
* :issue:`522` Outgoing proxy credentials can now be configured in the Recorder
* :issue:`527` Percentiles have been reworked to make more sense
* :issue:`530` New exponentially distributed pauses, thanks to Stephen Kuenzli (@skuenzli)
* :issue:`532` Add automatic request to compensate for engine warm up
* :issue:`535` Calling check() several times will now append them instead of overriding them

Fixes
-----

* :issue:`512` & :issue:`528` Fix class name resolution in gatling-maven-plugin, thanks to Cyril Couturi (@ccouturi) and Stephen Kuenzli (@skuenzli)
* :issue:`520` Add protection from SimpleAction crashes
* :issue:`534` Handle empty lines in CSV files

:version:`v1.1.6 <12>`
======================

Fixes
-----

* :issue:`498` Recorder: fix NPE on request body
* :issue:`507` gatling-maven-plugin: fix simulation package on Windows
* :issue:`508` Charts: fix encoding
* :issue:`510` Recorder: fix request bodies folder name

:version:`v1.1.5 <11>`
======================

Fixes
-----

* :issue:`489` Make recorder use relative URIs once connection established
* :issue:`490` Handle 303 redirect status code
* :issue:`491` Fix status code check when using non default one
* :issue:`497` Fix reports when request name contains "'"
* :issue:`498` Fix NPE in recorder when dumping request bodies
* :issue:`499` Fix latency chart
 
Features
--------

* :issue:`484` - Remove check logic from the AHC handler in order to reduce pressure on IO threads
* :issue:`486` - Charts: all session series is computed once for all
* :issue:`492` - Add a "maybe" check strategy when one want to capture an optional value
* :issue:`500` - Document transactions/sec chart
* :issue:`502` - Expose AHC configuration in Gatling conf

:version:`v1.1.4 <10>`
======================

Fixes
-----

* :issue:`481` Fix http client start up
* :issue:`483` Fix multiple simulations launching
 
Features
--------

* :issue:`485` - Charts: add new response time distribution
* :issue:`487` - EL: let occurrence be dynamic

:version:`v1.1.3 <9>`
======================

Fixes
-----

* :issue:`459` - Upgrade Netty 3.4.0.Final that fixes a compression bug.
* :issue:`460` - Fix recorder SSL certificate.
* :issue:`466` - Support relative Location headers
* :issue:`469` - Regression: the recorder shouldn't record Cookie and Content-Length headers
* :issue:`470` - Fix statistics displayed in the CLI while running

Features
--------

* :issue:`465` - Charts: display percentiles

:version:`v1.1.2 <8>`
======================

Fixes
-----

* :issue:`450` - Properly fixes cookie expiration
* :issue:`453` - Make XPathExtractor threadsafe
* :issue:`455` - Fix global statistics

Features
--------

* :issue:`327` - Akka 2 migration, wouhou!!!

:version:`v1.1.1 <7>`
======================

Fixes
-----

* :issue:`442` - Fixes fileBody templating
* :issue:`444` - Fixes cookie deletion

Features
--------

* :issue:`447` - Log at debug level the response on failed check

:version:`v1.1.0 <2>`
=====================

.. warning:: Migration required, see :ref:`migration guide <1.1.0-migration>`

Features
--------

* Engine

  * configurable run id and run description, see :issue:`416`
  * periodic statistic display while running, see :issue:`384`
  * link to generated reports, see :issue:`383`

* Check API

  * Check API is now type safe
  * optional transform step on extraction result
  * new JSONPath, see :issue:`433`
  * xpath namespaces support, see :issue:`434`

* Feeder API

  * new JDBC feeders for retrieving data from a SGBDR, see :issue:`37`
  * escape character support on CSV based feeders, see :issue:`105`
  * circular feeders, see :issue:`321`

* HTTP API

  * follow redirect support, see :issue:`105`
  * clean cookie handling, see :issue:`396`

* Charts API

  * configurable time window, see :issue:`323`
  * new active transactions/sec over time chart
  * new response latency over time chart

* Recorder

  * no longer an ubber jar, now to be launched from a script
  * follow redirect support
  * configurable generated Simulation package and class name, see :issue:`438`
  * configurable encoding, see :issue:`386`

* Extensions

  * new gatling-maven-plugin, contributed by @nhuray
  * new gatling debian package, contributed by @nhuray

And tons of bug fixes and performance enhancements!

:version:`v1.0.3 <5>` - Bug fix
===============================

Fixes
-----

* Fix a bug  in the recorder introduced in 1.0.2 that prevent from recording scenarios with less than 100 requests

:version:`v1.0.2 <4>` - Bug fix
===============================

Features
--------

* :issue:`345`, :issue:`348` & :issue:`330` - Better support for long scenarios (via :ref:`manual splitting <long-scenarios>`)
* :issue:`347` - Recorder splits long scenarios so they can be run with no extra configuration.
  

:version:`v1.0.1 <3>` - Bug fix
===============================

Fixes
-----

* :issue:`334` - Fixes reports template resolution under Windows
* :issue:`320` - Stops scenario if queue feeder not big enough
* Fixes a bug with empty lines at end of feeders

Features
--------

* Better CLI feedback

:version:`v1.0.0 <1>` - Initial Release
========================================