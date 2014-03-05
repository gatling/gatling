*********
Changelog
*********

`v1.5.3 <https://github.com/excilys/gatling/issues?milestone=42&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#1245 <https://github.com/excilys/gatling/issues/1245>`__ HTTP: Fix
   cookie RFC6265 style config parameter
-  `#1251 <https://github.com/excilys/gatling/issues/1251>`__ Doc: Fix
   samples that were targeting old CloudFoundry hosted sample
-  `#1301 <https://github.com/excilys/gatling/issues/1301>`__ HTTP: Fix
   default cookie path computation
-  `#1333 <https://github.com/excilys/gatling/issues/1333>`__ and
   `#1334 <https://github.com/excilys/gatling/issues/1334>`__ HTTP:
   ByteBufferInputStream fixes
-  `#1373 <https://github.com/excilys/gatling/issues/1373>`__ Charts:
   Times are round up too early
-  `#1508 <https://github.com/excilys/gatling/issues/1508>`__ HTTP:
   Revert default maxRetry to 4, better handle better connection is
   closed by the server after fetching from the pool but before writing
   the request

Features
~~~~~~~~

-  `#1447 <https://github.com/excilys/gatling/issues/1447>`__ HTTP:
   Upgrade Jodd 3.4.8
-  `#1452 <https://github.com/excilys/gatling/issues/1452>`__ HTTP:
   Upgrade AHC 1.7.21
-  `#1487 <https://github.com/excilys/gatling/issues/1487>`__ HTTP:
   Upgrade Netty 3.8.0
-  `#1531 <https://github.com/excilys/gatling/issues/1531>`__ HTTP:
   Upgrade Jsoup 1.7.3

`v2.0.0-M3a <https://github.com/excilys/gatling/issues?milestone=43&state=closed>`__
--------------------------------------------------------------------------------------

    This version fixes several major bugs in 2.0.0-M3, users are
    recommended to upgrade (transparent from M3)

Fixes
~~~~~

-  `#1281 <https://github.com/excilys/gatling/issues/1281>`__ Core:
   XPath check fails, ByteBufferInputStream buffer overflow
-  `#1282 <https://github.com/excilys/gatling/issues/1282>`__ Core:
   Simulation doesn't terminate properly
-  `#1283 <https://github.com/excilys/gatling/issues/1283>`__ HTTP:
   Wrong time out
-  `#1286 <https://github.com/excilys/gatling/issues/1286>`__ HTTP:
   Header regex check fails with NPE

`v2.0.0-M3 <https://github.com/excilys/gatling/issues?milestone=36&state=closed>`__
-------------------------------------------------------------------------------------

This version also contains all the fixes and features from 1.5.1 and
1.5.2. > Beware of the breaking changes since 2.0.0-M2, like protocol
set up and HTTP request bodies, see Gatling 2 page.

Fixes
~~~~~

-  `#1140 <https://github.com/excilys/gatling/issues/1140>`__ Core: Fix
   during loop
-  `#1148 <https://github.com/excilys/gatling/issues/1148>`__ Core:
   Remove Expression type alias that messes up with implicit conversions
-  `#1153 <https://github.com/excilys/gatling/issues/1153>`__ Recorder:
   Bug with URI parsing
-  `#1154 <https://github.com/excilys/gatling/issues/1154>`__ Recorder:
   Protect headers
-  `#1183 <https://github.com/excilys/gatling/issues/1183>`__ Charts:
   Incorrect OK/KO counts on groups
-  `#1207 <https://github.com/excilys/gatling/issues/1207>`__ HTTP:
   Additional CRLF when no Content-Disposition in multipart
-  `#1219 <https://github.com/excilys/gatling/issues/1219>`__ Recorder:
   Protected values in HAR files
-  `#1224 <https://github.com/excilys/gatling/issues/1224>`__ Core:
   Don't eagerly fetch resources from classpath
-  `#1243 <https://github.com/excilys/gatling/issues/1243>`__ Core: Div
   by 0 in ramp injection
-  `#1244 <https://github.com/excilys/gatling/issues/1244>`__ Charts:
   Percentage round up
-  `#1257 <https://github.com/excilys/gatling/issues/1257>`__ Core:
   TryMax exit condition
-  `#1259 <https://github.com/excilys/gatling/issues/1259>`__ Core:
   Wrong behavior when existASAP = false (repeat, foreach)
-  `#1266 <https://github.com/excilys/gatling/issues/1266>`__ Core:
   TryMax doesn't log inner groups
-  `#1270 <https://github.com/excilys/gatling/issues/1270>`__ App: Wrong
   behavior when simulation compiled class already exists

Features
~~~~~~~~

-  `#62 <https://github.com/excilys/gatling/issues/62>`__ and
   `#1142 <https://github.com/excilys/gatling/issues/1142>`__ JDBC:
   Store stats in RDBMS, contributed by Jay Patel (@jaypatel512)
-  `#1141 <https://github.com/excilys/gatling/pull/1141>`__ Core:
   Simulation set up refactoring
-  `#1083 <https://github.com/excilys/gatling/issues/1083>`__,
   `#1084 <https://github.com/excilys/gatling/issues/1084>`__ and
   `#1208 <https://github.com/excilys/gatling/issues/1208>`__ Core:
   Support loading resources (feeder, bodies) from classpath
-  `#1163 <https://github.com/excilys/gatling/issues/1163>`__ and
   `#1166 <https://github.com/excilys/gatling/issues/1166>`__ HTTP:
   Multipart support
-  `#1176 <https://github.com/excilys/gatling/issues/1176>`__ Recorder:
   Generate inline queries instead of queryParams
-  `#1181 <https://github.com/excilys/gatling/issues/1181>`__ Charts:
   Display KO percentages
-  `#1182 <https://github.com/excilys/gatling/issues/1182>`__ and
   `#1265 <https://github.com/excilys/gatling/issues/1265>`__ Recorder:
   Handle pauses around tags, contributed by Sébastien Keller (@Skeebl)
-  `#1185 <https://github.com/excilys/gatling/issues/1185>`__ Core:
   Expose XML parser config in gatling.conf
-  `#1211 <https://github.com/excilys/gatling/issues/1211>`__ HTTP: Drop
   Scalate's SSP
-  `#1226 <https://github.com/excilys/gatling/issues/1226>`__ HTTP: Make
   SSL store type optional
-  `#1237 <https://github.com/excilys/gatling/issues/1237>`__ HTTP: Make
   request bodies cache configurable
-  `#1252 <https://github.com/excilys/gatling/issues/1252>`__ Charts:
   Add errors distribution table
-  `#1269 <https://github.com/excilys/gatling/issues/1269>`__ HTTP:
   Support Grizzly as HTTP provider

`v1.5.2 <https://github.com/excilys/gatling/issues?milestone=41&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#1203 <https://github.com/excilys/gatling/issues/1203>`__ Recorder:
   Recorder doesn't record binary bodies correctly
-  `#1204 <https://github.com/excilys/gatling/issues/1204>`__ Core: CSV
   parser escapeChar doesn't work properly
-  `#1213 <https://github.com/excilys/gatling/issues/1213>`__ Maven:
   When skipping, maven plugin shouldn't parse args
-  `#1214 <https://github.com/excilys/gatling/issues/1214>`__ Core: Race
   condition in DataWriter.uninitialized
-  `#1216 <https://github.com/excilys/gatling/issues/1216>`__ HTTP: Have
   query param support both "foo=" and "foo" forms
-  `#1223 <https://github.com/excilys/gatling/issues/1223>`__ Core:
   JsonPath with array element NPE when the array is actually null
-  `#1225 <https://github.com/excilys/gatling/issues/1225>`__ Charts:
   Dots in javascript variable names make jQuery crash

Features
~~~~~~~~

-  `#1209 <https://github.com/excilys/gatling/issues/1209>`__ HTTP:
   Gatling doesn't honor RFC6265 cookie encoding style
-  `#1217 <https://github.com/excilys/gatling/issues/1217>`__ HTTP:
   Support AHC maxConnectionLifeTime
-  `#1221 <https://github.com/excilys/gatling/issues/1221>`__ HTTP: HEAD
   verb can have a body
-  `#1222 <https://github.com/excilys/gatling/issues/1222>`__ HTTP: Add
   OPTIONS verb support
-  `#1235 <https://github.com/excilys/gatling/issues/1235>`__ HTTP:
   Expose Virtual Host configuration
-  `#1240 <https://github.com/excilys/gatling/issues/1240>`__ HTTP: Add
   HttpProtocol.baseHeaders(headers)

Features
~~~~~~~~

`v1.5.1 <https://github.com/excilys/gatling/issues?milestone=40&state=closed>`__
----------------------------------------------------------------------------------

    From this version, one can: \* use ``-Dgatling.test.skip`` in order
    to disable the gatling maven plugin \* chose between ``jodd`` and
    ``jsoup`` in ``gatling.conf`` for the CSS Selector engine \* display
    multiple Simulations (launched in multiple maven plugin executions
    for example) in the Jenkins plugin

Fixes
~~~~~

-  `#1139 <https://github.com/excilys/gatling/issues/1139>`__ Core:
   Round-Robin feeder memory leak
-  `#1146 <https://github.com/excilys/gatling/issues/1146>`__ HTTP: Set
   AHC maxRetry to 0 by default
-  `#1155 <https://github.com/excilys/gatling/issues/1155>`__ Recorder:
   Recorder doesn't dump with selected encoding
-  `#1157 <https://github.com/excilys/gatling/issues/1157>`__ Recorder:
   Recorder doesn't properly extract bodies

Features
~~~~~~~~

-  `#1063 <https://github.com/excilys/gatling/issues/1063>`__ Maven:
   Allow one to skip gatling tests
-  `#1138 <https://github.com/excilys/gatling/issues/1138>`__ HTTP:
   Backport support for poorly encoded Location header
-  `#1145 <https://github.com/excilys/gatling/issues/1145>`__ Core: Let
   exec take a Scenario
-  `#1160 <https://github.com/excilys/gatling/issues/1160>`__ HTTP: Add
   session to log triggered by request failure
-  `#1162 <https://github.com/excilys/gatling/issues/1162>`__ Jenkins:
   Multiple gatling runs in single jenkins job, thanks to @cprice-puppet
-  `#1170 <https://github.com/excilys/gatling/issues/1170>`__ Core:
   Revive Jodd support, make it default implementation
-  `#1171 <https://github.com/excilys/gatling/issues/1171>`__ Core:
   Backport extractors caching to 1.5

`v2.0.0-M2 <https://github.com/excilys/gatling/issues?milestone=35&state=closed>`__
-------------------------------------------------------------------------------------

This version also contains all the fixes and features from 1.5.0.

Fixes
~~~~~

-  `#1093 <https://github.com/excilys/gatling/issues/1093>`__ Charts:
   Broken link if request name contains
-  `#1098 <https://github.com/excilys/gatling/issues/1098>`__ App: Fix
   regex for scala-compiler jar, thanks to @nap-stig
-  `#1124 <https://github.com/excilys/gatling/issues/1124>`__ Charts:
   Invalid JSON file
-  `#1136 <https://github.com/excilys/gatling/issues/1136>`__ Core: else
   branch of ifOrElse always ignored

Features
~~~~~~~~

-  `#827 <https://github.com/excilys/gatling/issues/827>`__ and
   `#1085 <https://github.com/excilys/gatling/issues/1085>`__ Recorder:
   Generate Simulation from HAR file
-  `#966 <https://github.com/excilys/gatling/issues/966>`__ and
   `#1090 <https://github.com/excilys/gatling/issues/1090>`__ Output
   final stats to console, thanks to @jaypatel512
-  `#1091 <https://github.com/excilys/gatling/issues/1091>`__ Core:
   ``dirac`` renamed into ``heaviside``
-  `#1105 <https://github.com/excilys/gatling/issues/1105>`__ and
   `#1113 <https://github.com/excilys/gatling/issues/1113>`__ Add an
   ``exitASAP`` to ``during`` and ``asLongAs`` loops so that exit
   condition is evaluated on every action instead of once per iteration

`v1.5.0 <https://github.com/excilys/gatling/issues?milestone=38&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#1094 <https://github.com/excilys/gatling/issues/1094>`__ HTTP:
   JsonPath and XPath checks consume the response body
-  `#1095 <https://github.com/excilys/gatling/issues/1095>`__ Charts:
   Charts names encoding problem
-  `#1116 <https://github.com/excilys/gatling/issues/1116>`__ Core:
   DataWriter race condition
-  `#1127 <https://github.com/excilys/gatling/issues/1127>`__ Recorder:
   Invalid dot in chains
-  `#1133 <https://github.com/excilys/gatling/issues/1133>`__ HTTP:
   Don't drop query params inlined in the URL

Features
~~~~~~~~

-  `#1055 <https://github.com/excilys/gatling/issues/1055>`__ Core:
   Reorganize gatling.conf
-  `#1099 <https://github.com/excilys/gatling/issues/1099>`__ Core:
   Override ``gatling.data.writers`` from System properties
-  `#1101 <https://github.com/excilys/gatling/issues/1101>`__ and
   `#1102 <https://github.com/excilys/gatling/issues/1102>`__ Recorder:
   Override default keystore, thanks to @cprice-puppet
-  `#1114 <https://github.com/excilys/gatling/issues/1114>`__ Core:
   Switch from Jodd to Jsoup as CSS selectors implementation
-  `#1115 <https://github.com/excilys/gatling/issues/1115>`__ Drop
   custom JsonPath in favor of Jayway one
-  `#1117 <https://github.com/excilys/gatling/issues/1117>`__ HTTP: Add
   ``HttpProtocolConfiguration.shareClient`` to have one http client per
   user (disabled by default)
-  `#1119 <https://github.com/excilys/gatling/issues/1119>`__ HTTP: Add
   ``HttpProtocolConfiguration.shareConnections`` to have one http
   client per user (enabled by default)
-  `#1121 <https://github.com/excilys/gatling/issues/1121>`__ HTTP: Add
   Session attributes for setting SSL Engine per user
-  `#1125 <https://github.com/excilys/gatling/issues/1125>`__ Core: Add
   a light mode for ConsoleDataWriter
-  `#1126 <https://github.com/excilys/gatling/issues/1126>`__ Graphite:
   Add a light mode for GraphiteDataWriter
-  `#1134 <https://github.com/excilys/gatling/issues/1135>`__ and
   (https://github.com/excilys/gatling/issues/) App: Allow simulation
   description to be configurable via conf file / CLI, thanks to
   @cprice-puppet
-  `#1137 <https://github.com/excilys/gatling/issues/1137>`__ Refactor
   doIfOrElse(String, String) into doIfEqualsOfElse(Expression[String],
   Expression[String])

    Note: Beware that the configuration file has changed, so the System
    property names to override it too! Please check the new file.

`v2.0.0-M1 <https://github.com/excilys/gatling/issues?milestone=23&state=closed>`__
-------------------------------------------------------------------------------------

See full `milestone
content <https://github.com/excilys/gatling/issues?milestone=23&state=closed>`__
and [[Gatling 2]] page.

`v1.4.7 <https://github.com/excilys/gatling/issues?milestone=37&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#1047 <https://github.com/excilys/gatling/issues/1047>`__ and
   `#1049 <https://github.com/excilys/gatling/issues/1049>`__ Maven:
   Forked JVM might not exit on Windows, thanks to alvinlin123!
-  `#1066 <https://github.com/excilys/gatling/issues/1066>`__ Maven:
   System properties propagation still not working under Windows
-  `#1074 <https://github.com/excilys/gatling/issues/1074>`__ Charts:
   Broken records cause generation to fail
-  `#1080 <https://github.com/excilys/gatling/issues/1080>`__ Charts:
   Javascript error when request name starts with a number

Features
~~~~~~~~

-  `#1050 <https://github.com/excilys/gatling/issues/1050>`__ Core:
   Upgrade Slf4j 1.7.5
-  `#1051 <https://github.com/excilys/gatling/issues/1051>`__ Core:
   Upgrade Logback 1.0.11
-  `#1070 <https://github.com/excilys/gatling/issues/1070>`__ Charts:
   Long request names break the menu
-  `#1072 <https://github.com/excilys/gatling/issues/1072>`__ HTTP:
   Upgrade AsyncHttpClient 1.7.13
-  `#1073 <https://github.com/excilys/gatling/issues/1073>`__ HTTP:
   Upgrade Netty 3.6.5

`v1.4.6 <https://github.com/excilys/gatling/issues?milestone=34&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#1018 <https://github.com/excilys/gatling/issues/1018>`__ and
   `#1019 <https://github.com/excilys/gatling/issues/1019>`__ Core:
   Possible race condition on initialization
-  `#1020 <https://github.com/excilys/gatling/issues/1020>`__ HTTP: Warm
   up done twice, slowing down start up
-  `#1037 <https://github.com/excilys/gatling/issues/1037>`__ Maven:
   Renaming property ``simulation`` into ``simulationClass`` in order to
   avoid clash when passing as System property
-  `#1042 <https://github.com/excilys/gatling/issues/1042>`__ Recorder:
   Invalid generated simulation when first element is a tag
-  `#1044 <https://github.com/excilys/gatling/issues/1044>`__ Maven:
   Fails when propagating a System property with a space

Features
~~~~~~~~

-  `#1046 <https://github.com/excilys/gatling/issues/1046>`__ Core: Drop
   attribute axis support in JsonPath, introduce magic value ``_`` to
   target root array

    *Note*: Be aware of the 2 breaking changes: \* Maven plugin property
    ``simulation`` renamed into ``simulationClass`` \* JsonPath not
    longer supports attribute axis (didn't really make sense and was
    equivalent to child element one).

`v1.4.5 <https://github.com/excilys/gatling/issues?milestone=33&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#995 <https://github.com/excilys/gatling/issues/995>`__ and
   `#1013 <https://github.com/excilys/gatling/issues/1013>`__ Core:
   Can't use assertions with "manual scaling" procedure, see
   `wiki <https://github.com/excilys/gatling/wiki/Scaling-out>`__
-  `#1003 <https://github.com/excilys/gatling/issues/1003>`__ Maven:
   line.separator disappears when propagation System properties
-  `#1009 <https://github.com/excilys/gatling/issues/1009>`__ Core:
   config doesn't get overridden with System properties

Features
~~~~~~~~

-  `#1017 <https://github.com/excilys/gatling/issues/1017>`__ HTTP:
   Upgrade to `async-http-client
   1.7.12 <https://github.com/AsyncHttpClient/async-http-client/issues?milestone=2&page=1&state=closed>`__

`v1.4.4 <https://github.com/excilys/gatling/issues?milestone=32&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#971 <https://github.com/excilys/gatling/issues/971>`__ Maven:
   Plugin broken, wrong Scala version resolved
-  `#974 <https://github.com/excilys/gatling/issues/974>`__ HTTP: XPath
   and JsonPath checks crash on empty body
-  `#984 <https://github.com/excilys/gatling/issues/984>`__ and
   `#993 <https://github.com/excilys/gatling/issues/993>`__ Charts:
   stats.tsv file broken

Features
~~~~~~~~

-  `#906 <https://github.com/excilys/gatling/issues/906>`__, \*
   `#911 <https://github.com/excilys/gatling/issues/911>`__ and \*
   `#972 <https://github.com/excilys/gatling/issues/972>`__ Core: add
   new ``foreach`` DSL, see
   `doc <https://github.com/excilys/gatling/wiki/Structure-Elements#wiki-foreach>`__
-  `#960 <https://github.com/excilys/gatling/issues/960>`__ Core: Make
   user ids unique
-  `#977 <https://github.com/excilys/gatling/issues/977>`__ Maven:
   Propagate System properties in fork mode
-  `#983 <https://github.com/excilys/gatling/issues/983>`__ Charts: Make
   Graphite root configurable, defaulting to gatling
-  `#996 <https://github.com/excilys/gatling/issues/996>`__ HTTP: Make
   SslContext configurable

-  tons of optimizations, see full issues list for details

`v1.4.3 <https://github.com/excilys/gatling/issues?milestone=31&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#914 <https://github.com/excilys/gatling/issues/914>`__,
   `#915 <https://github.com/excilys/gatling/issues/915>`__,
   `#918 <https://github.com/excilys/gatling/issues/918>`__,
   `#919 <https://github.com/excilys/gatling/issues/919>`__ Charts:
   Charts not rendering when request/group name contains special
   characters
-  `#922 <https://github.com/excilys/gatling/issues/922>`__ Core: Fix
   pause shift computation
-  `#926 <https://github.com/excilys/gatling/issues/926>`__ HTTP:
   Improve domain matching in cookie handling
-  `#942 <https://github.com/excilys/gatling/issues/942>`__ Recorder:
   Handle poorly encoded queries
-  `#944 <https://github.com/excilys/gatling/issues/944>`__ Core: NPE
   when running a simulation with 2 scenarios with the same name
-  `#954 <https://github.com/excilys/gatling/issues/954>`__ and
   `#956 <https://github.com/excilys/gatling/issues/956>`__ HTTP:
   Filtered out multiple cookies with the same name/path (support PHP
   bug)

Features
~~~~~~~~

-  `#913 <https://github.com/excilys/gatling/issues/913>`__ HTTP:
   Upgrade Netty 3.6.2
-  `#934 <https://github.com/excilys/gatling/issues/934>`__ HTTP:
   Upgrade async-http-client 2.10
-  `#941 <https://github.com/excilys/gatling/issues/941>`__ Core: Loops
   clean up
-  `#957 <https://github.com/excilys/gatling/issues/957>`__ HTTP: Add
   Connection to possible common headers

`v1.4.2 <https://github.com/excilys/gatling/issues?milestone=30&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#881 <https://github.com/excilys/gatling/issues/881>`__ and
   `#910 <https://github.com/excilys/gatling/issues/910>`__ Core: Fix
   Zinc incremental compiler NPE
-  `#898 <https://github.com/excilys/gatling/issues/898>`__ Charts:
   invalid group OK/KO stats
-  `#899 <https://github.com/excilys/gatling/issues/899>`__ HTTP:
   Cookies are not propagated from HTTP to HTTPS
-  `#907 <https://github.com/excilys/gatling/issues/907>`__ JDBC:
   JdbcFeeder SQLException with Oracle

`v1.4.1 <https://github.com/excilys/gatling/issues?milestone=29&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#882 <https://github.com/excilys/gatling/issues/882>`__ Core:
   Possible SOE when too many requests
-  `#884 <https://github.com/excilys/gatling/issues/884>`__ Recorder:
   Not working with HTTPS
-  `#886 <https://github.com/excilys/gatling/issues/886>`__ Charts:
   Wrong statistics for groups
-  `#893 <https://github.com/excilys/gatling/issues/893>`__ Maven:
   reportsOnly not work with maven plugin

Features
~~~~~~~~

-  `#889 <https://github.com/excilys/gatling/issues/889>`__ Core:
   Upgrade Scala 2.9.3-RC1
-  `#892 <https://github.com/excilys/gatling/issues/892>`__ Core:
   Upgrade Akka 2.0.5
-  `#894 <https://github.com/excilys/gatling/issues/894>`__ HTTP:
   Upgrade Netty 3.6.1

`v1.4.0 <https://github.com/excilys/gatling/issues?milestone=22&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#844 <https://github.com/excilys/gatling/issues/844>`__ Charts:
   Handle parentheses in request names
-  `#846 <https://github.com/excilys/gatling/issues/846>`__ Core: Fix
   possible NPE in FileDataWriter.sanitize
-  `#866 <https://github.com/excilys/gatling/issues/866>`__
   `#867 <https://github.com/excilys/gatling/issues/867>`__ Charts: Fix
   stats.tsv file header, thanks to @caps (Pete Capra)

Features
~~~~~~~~

-  `#170 <https://github.com/excilys/gatling/issues/170>`__
   `#322 <https://github.com/excilys/gatling/issues/322>`__ Charts: New
   API for grouping requests
-  `#560 <https://github.com/excilys/gatling/issues/560>`__ Core: New
   API for acceptance criteria
-  `#594 <https://github.com/excilys/gatling/issues/594>`__ Maven: New
   Jenkins plugin
-  `#772 <https://github.com/excilys/gatling/issues/772>`__ Charts:
   Reorganize description
-  `#782 <https://github.com/excilys/gatling/issues/782>`__ Core: 1.3.X
   deprecated APIs removed
-  `#788 <https://github.com/excilys/gatling/issues/788>`__
   `#810 <https://github.com/excilys/gatling/issues/810>`__ Core:
   Jackson fully configurable through gatling.conf
-  `#802 <https://github.com/excilys/gatling/issues/802>`__ HTTP: Host
   header doesn't have to be specified as AHC computes it
-  `#829 <https://github.com/excilys/gatling/issues/829>`__ Core: Feeder
   is now Iterator[Map[String, T]]
-  `#832 <https://github.com/excilys/gatling/issues/832>`__ Charts:
   Upgrade jQuery 1.8.3
-  `#838 <https://github.com/excilys/gatling/issues/838>`__
   `#840 <https://github.com/excilys/gatling/issues/840>`__ Core:
   Breaking change in Simulation structure: remove apply and configure,
   introduce setUp
-  `#839 <https://github.com/excilys/gatling/issues/839>`__ Maven: maven
   plugin now use src/test/scala and src/test/resources folders
-  `#841 <https://github.com/excilys/gatling/issues/841>`__ Core: New
   ``.size`` EL function
-  `#847 <https://github.com/excilys/gatling/issues/847>`__ Core: Make
   extraResponseInfoExtractor take an ExtendedResponse
-  `#848 <https://github.com/excilys/gatling/issues/848>`__
   `#879 <https://github.com/excilys/gatling/issues/879>`__ HTTP: Better
   tracing/debugging of requests and responses
-  `#849 <https://github.com/excilys/gatling/issues/849>`__ HTTP:
   Upgrade Netty 3.6.0.Final
-  `#857 <https://github.com/excilys/gatling/issues/857>`__ Core: Stop
   engine nicely instead of System.exit on feeder starvation
-  `#860 <https://github.com/excilys/gatling/issues/860>`__ Core:
   Upgrade Logback 1.0.9
-  `#861 <https://github.com/excilys/gatling/issues/861>`__ Core:
   Upgrade Jackson 2.1.2
-  `#864 <https://github.com/excilys/gatling/issues/864>`__
   `#872 <https://github.com/excilys/gatling/issues/872>`__ Maven:
   maven-gatling-plugin refactoring
-  `#870 <https://github.com/excilys/gatling/issues/870>`__ HTTP: Make
   fileBody dynamic
-  `#874 <https://github.com/excilys/gatling/issues/874>`__ Core: Fix
   Zinc when Gatling path contains special characters
-  `#876 <https://github.com/excilys/gatling/issues/876>`__ App: Rename
   deb package name into gatling-tool
-  `#877 <https://github.com/excilys/gatling/issues/877>`__ HTTP:
   Upgrade AHC 1.7.9, fix bug when no headers
-  `#880 <https://github.com/excilys/gatling/issues/880>`__ Charts:
   Upgrade Highcharts 2.3.5 and Highstock 1.2.5

    *Note*: This release introduce a breaking change in the Simulation
    classes format. Deprecated methods in 1.3.X have been removed. See
    `migration
    guide <https://github.com/excilys/gatling/wiki/Migrating#wiki-1.4.0>`__

`v1.3.5 <https://github.com/excilys/gatling/issues?milestone=28&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#799 <https://github.com/excilys/gatling/issues/799>`__ Maven:
   src/test/scala directory missing in projects generated with the
   archetype
-  `#800 <https://github.com/excilys/gatling/issues/800>`__ Core: Fix
   debig logger in logback.xml
-  `#808 <https://github.com/excilys/gatling/issues/808>`__ HTTP:
   Original ContentType header shouldn't be propagated when redirecting
-  `#813 <https://github.com/excilys/gatling/issues/813>`__ HTTP:
   followRedirect not working properly when Location contains a query
-  `#816 <https://github.com/excilys/gatling/issues/816>`__ HTTP:
   CookieStore not accounting for port number in domain computation
-  `#820 <https://github.com/excilys/gatling/issues/820>`__ Core:
   JsonPath not supporting wildcards

Features
~~~~~~~~

-  `#765 <https://github.com/excilys/gatling/issues/765>`__ and
   `#814 <https://github.com/excilys/gatling/issues/814>`__ HTTP: Print
   HTTP params in debug
-  `#792 <https://github.com/excilys/gatling/issues/792>`__ Core: Make
   request names dynamic
-  `#817 <https://github.com/excilys/gatling/issues/817>`__ HTTP:
   Authorization header can now be declared as a common header in
   HttpProtocolConfiguration
-  `#818 <https://github.com/excilys/gatling/issues/818>`__ HTTP:
   Support for non UTF-8 encoded cookies (value containing an "="
   character)

`v1.3.4 <https://github.com/excilys/gatling/issues?milestone=27&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#785 <https://github.com/excilys/gatling/issues/785>`__ Recorder:
   Fix followRedirect support
-  `#786 <https://github.com/excilys/gatling/issues/786>`__ Core: Fix
   during loop timer
-  `#787 <https://github.com/excilys/gatling/issues/787>`__ Core: Fix
   JsonPath

`v1.3.3 <https://github.com/excilys/gatling/issues?milestone=26&state=closed>`__
----------------------------------------------------------------------------------

Features
~~~~~~~~

-  `#754 <https://github.com/excilys/gatling/issues/754>`__ Core: Use
   Zinc incremental compiler
-  `#763 <https://github.com/excilys/gatling/issues/763>`__ and
   `#769 <https://github.com/excilys/gatling/issues/769>`__ Charts:
   Redesign statistic summary table
-  `#775 <https://github.com/excilys/gatling/issues/775>`__
   HTTP/Recorder: DNT common header support
-  `#779 <https://github.com/excilys/gatling/issues/779>`__ Core: Loop
   index is now directly exposed as Session attribute

Fixes
~~~~~

-  `#755 <https://github.com/excilys/gatling/issues/755>`__ Charts:
   Square and curly braces in request names mess up with javascript
-  `#756 <https://github.com/excilys/gatling/issues/756>`__ HTTP: Fix
   abusing caching
-  `#759 <https://github.com/excilys/gatling/issues/759>`__ Core: Fix
   check when forcing Simulation
-  `#760 <https://github.com/excilys/gatling/issues/760>`__ Maven: add
   missing logback.xml file
-  `#762 <https://github.com/excilys/gatling/issues/762>`__ HTTP:
   Support Expires headers numerical values
-  `#766 <https://github.com/excilys/gatling/issues/766>`__ Metrics: Max
   and count should be reseted along with the buckets
-  `#770 <https://github.com/excilys/gatling/issues/770>`__ Charts: Run
   description is not properly printed
-  `#777 <https://github.com/excilys/gatling/issues/777>`__ Core: Fix
   unrecoverable crash on while condition evaluation
-  `#778 <https://github.com/excilys/gatling/issues/778>`__ Core:
   Counter should be incremented before the loop content
-  `#780 <https://github.com/excilys/gatling/issues/780>`__ Recorder:
   Fix pauses shift

`v1.3.2 <https://github.com/excilys/gatling/issues?milestone=25&state=closed>`__
----------------------------------------------------------------------------------

Features
~~~~~~~~

-  `#750 <https://github.com/excilys/gatling/issues/750>`__ HTTP: better
   support of multivalued params and queryParams with multiValuedParam
   and multiValueQueryParam, see
   `wiki <https://github.com/excilys/gatling/wiki/HTTP#wiki-query-params>`__

Fixes
~~~~~

-  `#753 <https://github.com/excilys/gatling/issues/753>`__ HTTP:
   regression: params were being sent as query params

`v1.3.1 <https://github.com/excilys/gatling/issues?milestone=24&state=closed>`__
----------------------------------------------------------------------------------

Features
~~~~~~~~

-  `#743 <https://github.com/excilys/gatling/issues/743>`__ Checks: Add
   new bodyString check, see
   `wiki <https://github.com/excilys/gatling/wiki/Checks#http-response-body>`__
-  `#744 <https://github.com/excilys/gatling/issues/744>`__ HTTP:
   Upgrade Netty to 3.5.8.Final, critical performance fixes
-  `#752 <https://github.com/excilys/gatling/issues/752>`__ Config: Add
   aliases for built-ins data readers and writers: console, file and
   graphite

Fixes
~~~~~

-  `#732 <https://github.com/excilys/gatling/issues/732>`__ HTTP:
   responseChunksDiscardingEnabled was not working properly
-  `#734 <https://github.com/excilys/gatling/issues/734>`__ HTTP: Host
   header was being ignored, fixed thanks to @dustinbarnes
-  `#735 <https://github.com/excilys/gatling/issues/735>`__ Core: NPE
   when using chain
-  `#736 <https://github.com/excilys/gatling/issues/736>`__ Charts: Drop
   Scalding/Cascading, considerably reduce memory footprint, introduce
   accuracy config parameter defaulting to 10ms
-  `#745 <https://github.com/excilys/gatling/issues/745>`__ Recorder:
   Tags were not dumped in the generated Simulation
-  `#747 <https://github.com/excilys/gatling/issues/747>`__ Charts: Some
   charts disappear. This is due to a Highstock bug that has been fixed
   yet, so a workaround was introduced
-  `#751 <https://github.com/excilys/gatling/issues/751>`__ Feeders and
   Charts: File streams were not properly closed

    *Note*: Due to the new accuracy parameter, you will experience less
    precise values in the charts. You can get the old behavior by
    setting a 1 ms value, at the cost of a higher memory usage.

`v1.3.0 <https://github.com/excilys/gatling/issues?milestone=20&state=closed>`__
----------------------------------------------------------------------------------

    *Warning*: Migration required, see `migration
    guide <https://github.com/excilys/gatling/wiki/Migrating#wiki-1.3.0>`__

API changes:
~~~~~~~~~~~~

-  `#669 <https://github.com/excilys/gatling/issues/669>`__ Core: Config
   file format change
-  `#698 <https://github.com/excilys/gatling/issues/698>`__ Core:
   Durations expressed as (value, unit) are deprecated
-  `#699 <https://github.com/excilys/gatling/issues/699>`__ Core: Loops
   refactoring, old ones are deprecated
-  `#705 <https://github.com/excilys/gatling/issues/705>`__ Core:
   insertChain deprecated, use exec
-  `#711 <https://github.com/excilys/gatling/issues/711>`__ Core:
   Feeders are now Iterators
-  `#730 <https://github.com/excilys/gatling/issues/730>`__ Core: doIf
   refactoring

Features
~~~~~~~~

-  `#592 <https://github.com/excilys/gatling/issues/592>`__ Charts:
   Display mean number of requests/sec in global chart page
-  `#593 <https://github.com/excilys/gatling/issues/593>`__ Charts:
   Generate a csv file with global stats
-  `#604 <https://github.com/excilys/gatling/issues/604>`__ and
   `#672 <https://github.com/excilys/gatling/issues/672>`__ Charts:
   Response time and latency charts now display (min, max) ranges
   instead of a max values line
-  `#606 <https://github.com/excilys/gatling/issues/606>`__ HTTP: New
   byteArrayBody(Session => Array[Byte]), see
   `wiki <https://github.com/excilys/gatling/wiki/HTTP#wiki-byteArray-sessionbody>`__
-  `#607 <https://github.com/excilys/gatling/issues/607>`__ HTTP: New
   baseUrls for round-robin, thanks to @israel, see
   `wiki <https://github.com/excilys/gatling/wiki/HTTP#wiki-base-url>`__
-  `#607 <https://github.com/excilys/gatling/issues/607>`__ and
   `#683 <https://github.com/excilys/gatling/issues/683>`__ Charts: New
   summary table on global page
-  `#621 <https://github.com/excilys/gatling/issues/621>`__ Checks: Css
   checks underlying parser now supports browser conditional tests
-  `#623 <https://github.com/excilys/gatling/issues/623>`__ HTTP: New
   caching support, see
   `wiki <https://github.com/excilys/gatling/wiki/HTTP#wiki-caching>`__
-  `#624 <https://github.com/excilys/gatling/issues/624>`__ Core: New
   console dashboard
-  `#627 <https://github.com/excilys/gatling/issues/627>`__ Checks: New
   currentLocation check, see
   `wiki <https://github.com/excilys/gatling/wiki/Checks#wiki-location>`__
-  `#628 <https://github.com/excilys/gatling/issues/628>`__ Core: New
   pauseCustom(() => Long), see
   `wiki <https://github.com/excilys/gatling/wiki/Structure-Elements#wiki-pause>`__
-  `#641 <https://github.com/excilys/gatling/issues/641>`__ and
   `#658 <https://github.com/excilys/gatling/issues/658>`__ HTTP: Log
   requests and responses on failure
-  `#644 <https://github.com/excilys/gatling/issues/644>`__ HTTP:
   paramKey and fileName upload parameters are now dynamic
-  `#646 <https://github.com/excilys/gatling/issues/646>`__ HTTP: Multi
   file upload support, thanks to @codemnky
-  `#647 <https://github.com/excilys/gatling/issues/647>`__ and
   `#690 <https://github.com/excilys/gatling/issues/690>`__ Core: New
   randomSwitch, see
   `wiki <https://github.com/excilys/gatling/wiki/Structure-Elements#wiki-randomSwitch>`__
-  `#652 <https://github.com/excilys/gatling/issues/652>`__ HTTP: New
   disableResponseChunksDiscarding, see
   `wiki <https://github.com/excilys/gatling/wiki/HTTP#wiki-custom-dump>`__
-  `#652 <https://github.com/excilys/gatling/issues/652>`__ Checks: Css
   checks now support attribute node selection, see
   `wiki <https://github.com/excilys/gatling/wiki/Checks#wiki-css>`__
-  `#674 <https://github.com/excilys/gatling/issues/674>`__ and
   `#675 <https://github.com/excilys/gatling/issues/675>`__ Graphite:
   Gatling can now report to Graphite, see wiki
-  `#685 <https://github.com/excilys/gatling/issues/685>`__ Project:
   Continuous Integration now on Cloudbees
-  `#688 <https://github.com/excilys/gatling/issues/688>`__ Charts: New
   polar chart with request counts
-  `#701 <https://github.com/excilys/gatling/issues/701>`__ Core: New
   exitBlockOnFail and exitHereIfFailed, see
   `wiki <https://github.com/excilys/gatling/wiki/Structure-Elements#wiki-exitBlockOnFail>`__
-  `#702 <https://github.com/excilys/gatling/issues/702>`__ Core: New
   tryMax, see
   `wiki <https://github.com/excilys/gatling/wiki/Structure-Elements#wiki-tryMax>`__
-  `#703 <https://github.com/excilys/gatling/issues/703>`__ Core: Remove
   bootstrapping from chain, see
   `wiki <https://github.com/excilys/gatling/wiki/Structure-Elements#wiki-bootstrap>`__
-  `#706 <https://github.com/excilys/gatling/issues/706>`__ Core: new
   randomRoundRobin, see
   `wiki <https://github.com/excilys/gatling/wiki/Structure-Elements#wiki-roundRobinSwitch>`__
-  `#712 <https://github.com/excilys/gatling/issues/712>`__ Core: Let
   exec take a chain vararg, see
   `wiki <https://github.com/excilys/gatling/wiki/Structure-Elements#wiki-exec>`__
-  `#714 <https://github.com/excilys/gatling/issues/714>`__ Core: Better
   simulations compilation warnings

Fixes
~~~~~

-  `#571 <https://github.com/excilys/gatling/issues/571>`__ HTTP: Better
   cookies support
-  `#609 <https://github.com/excilys/gatling/issues/609>`__ HTTP: NPE
   when Location header missing
-  `#615 <https://github.com/excilys/gatling/issues/615>`__ HTTP: Url is
   encoded twice on redirect
-  `#630 <https://github.com/excilys/gatling/issues/630>`__ Charts: Fix
   percentiles
-  `#639 <https://github.com/excilys/gatling/issues/639>`__ and
   `#687 <https://github.com/excilys/gatling/issues/687>`__ Recorder:
   should ask before overwriting
-  `#651 <https://github.com/excilys/gatling/issues/651>`__ Check:
   responseTime and latency checks mustn't cause body to be stored
-  `#653 <https://github.com/excilys/gatling/issues/653>`__ HTTP:
   Duration computation problems
-  `#664 <https://github.com/excilys/gatling/issues/664>`__ Core: Don't
   display Abstract simulations
-  `#665 <https://github.com/excilys/gatling/issues/665>`__ Core:
   LinkageError when using inheritance
-  `#709 <https://github.com/excilys/gatling/issues/709>`__ Recorder:
   support empty valued parameters
-  `#713 <https://github.com/excilys/gatling/issues/713>`__ and
   `#715 <https://github.com/excilys/gatling/issues/715>`__ Charts:
   support quotes in request names

`v1.2.5 <https://github.com/excilys/gatling/issues?milestone=21&state=closed>`__
----------------------------------------------------------------------------------

Features
~~~~~~~~

-  `#596 <https://github.com/excilys/gatling/issues/596>`__ Better live
   informations

Fixes
~~~~~

-  `#597 <https://github.com/excilys/gatling/issues/597>`__ Fix cookie
   handling regression
-  `#599 <https://github.com/excilys/gatling/issues/599>`__ Time
   measurement is intrinsically imprecise, ensure that it can't cause
   negative response times
-  `#600 <https://github.com/excilys/gatling/issues/600>`__ Fix response
   time distribution chart, max value wasn't properly displayed
-  `#601 <https://github.com/excilys/gatling/issues/601>`__ Fix
   gatling-maven-plugin JVM arguments

`v1.2.4 <https://github.com/excilys/gatling/issues?milestone=16&state=closed>`__
----------------------------------------------------------------------------------

Features
~~~~~~~~

-  `#446 <https://github.com/excilys/gatling/issues/446>`__ Add the
   ability to dump custom data in the logs, thanks to Stephen Kuenzli,
   see
   `wiki <https://github.com/excilys/gatling/wiki/HTTP#wiki-custom-dump>`__
-  `#569 <https://github.com/excilys/gatling/issues/569>`__ New
   reponseTimeInMillis and latencyInMillis checks, see
   `wiki <https://github.com/excilys/gatling/wiki/Checks#wiki-response-time>`__
-  `#576 <https://github.com/excilys/gatling/issues/576>`__ new
   headerRegex check, see
   `wiki <https://github.com/excilys/gatling/wiki/Checks#wiki-header-regex>`__
-  `#591 <https://github.com/excilys/gatling/issues/591>`__ Location
   header is now automatically decoded when checked, see
   `wiki <https://github.com/excilys/gatling/wiki/Checks#wiki-header>`__
-  `#595 <https://github.com/excilys/gatling/issues/595>`__ New simple
   feeder, see
   `wiki <https://github.com/excilys/gatling/wiki/Feeders#wiki-simple>`__

Fixes
~~~~~

-  `#572 <https://github.com/excilys/gatling/issues/572>`__ Fix a bug
   where cookies with the same name could be sent both under certain
   conditions
-  `#573 <https://github.com/excilys/gatling/issues/573>`__ Fix script
   variables scope under Windows, thanks to Henri Tremblay
-  `#574 <https://github.com/excilys/gatling/issues/574>`__ Fix logger
   in logback.conf, thanks to Henri Tremblay
-  `#583 <https://github.com/excilys/gatling/issues/583>`__ Fix engine
   encoding handling
-  `#586 <https://github.com/excilys/gatling/issues/586>`__ Fix recorder
   class name and package generation
-  `#587 <https://github.com/excilys/gatling/issues/587>`__ Fix recorder
   encoding handling

`v1.2.3 <https://github.com/excilys/gatling/issues?milestone=15&state=closed>`__
----------------------------------------------------------------------------------

Fixes
~~~~~

-  `#566 <https://github.com/excilys/gatling/issues/566>`__ Fix body
   checks regression in 1.2.2

`v1.2.2 <https://github.com/excilys/gatling/issues?milestone=14&state=closed>`__
----------------------------------------------------------------------------------

    *Warning*: due to566, 1.2.2 users are recommended to upgrade to
    1.2.3

Features
~~~~~~~~

-  `#543 <https://github.com/excilys/gatling/issues/543>`__ Make charts
   generation consume multiple simulation(.\*).log files, ease multiple
   instances testing, see
   `wiki <https://github.com/excilys/gatling/wiki/Scaling-out>`__
-  `#548 <https://github.com/excilys/gatling/issues/548>`__ New
   `Redis <http://redis.io>`__ Feeder, thanks to @krishnenc (Krishnen
   Chedambarum), see
   `wiki <https://github.com/excilys/gatling/wiki/Feeders#wiki-redis>`__
-  `#548 <https://github.com/excilys/gatling/issues/548>`__ New
   byteArrayBody, thanks to @krishnenc (Krishnen Chedambarum), see
   `wiki <https://github.com/excilys/gatling/wiki/HTTP#wiki-request-body>`__
-  `#552 <https://github.com/excilys/gatling/issues/552>`__ Gatling
   modules can now be built independantly, thanks to @nire (Nicolas
   Rémond)
-  `#553 <https://github.com/excilys/gatling/issues/553>`__ New checksum
   checks, see
   `wiki <https://github.com/excilys/gatling/wiki/Checks#wiki-checksum>`__
-  `#555 <https://github.com/excilys/gatling/issues/555>`__ Run name can
   now be set on gatling-maven-plugin, see
   `wiki <https://github.com/excilys/gatling/wiki/Maven-plugin#wiki-advanced>`__
-  `#557 <https://github.com/excilys/gatling/issues/557>`__ Gatling now
   ships `Grizzly <http://grizzly.java.net>`__ to ease switching NIO
   provider (Gatling still uses Netty by default)

Fixes
~~~~~

-  `#562 <https://github.com/excilys/gatling/issues/562>`__ Fix
   gatling-maven-plugin crash when setting no-reports or run-name
   options, thanks to @skuenzli (Stephen Kuenzli)
-  `#558 <https://github.com/excilys/gatling/issues/558>`__ Ensure
   IfAction and WhileAction don't lose messages on crash

`v1.2.1 <https://github.com/excilys/gatling/issues?milestone=13&state=closed>`__
----------------------------------------------------------------------------------

    *Warning*: Due to545, 1.2.0 users are highly recommended to
    upgrade!

Features
~~~~~~~~

-  `#539 <https://github.com/excilys/gatling/issues/539>`__ much better
   reports generation memory footprint

Fixes
~~~~~

-  `#536 <https://github.com/excilys/gatling/pull/536>`__
   HttpProtocolConfiguration DSL wouldn't compile when proxy was defined
   in the middle of the chain
-  `#537 <https://github.com/excilys/gatling/pull/537>`__ Warm up
   request would break the run when target url cannot be reached
-  `#538 <https://github.com/excilys/gatling/pull/538>`__ Fix scatter
   plot chart
-  `#540 <https://github.com/excilys/gatling/pull/540>`__ Fix percentile
   ordinal suffix, thanks to Nicolas Rémond
-  `#544 <https://github.com/excilys/gatling/pull/544>`__ Fix times
   computation at very high throughput
-  `#545 <https://github.com/excilys/gatling/pull/545>`__ Fix pause
   duration
-  `#546 <https://github.com/excilys/gatling/pull/546>`__ Fix launch
   script when path contains special characters, thanks to Jean-François
   Bilger

`v1.2.0 <https://github.com/excilys/gatling/issues?milestone=6&state=closed>`__
---------------------------------------------------------------------------------

    *Warning*: Migration required, see `migration
    guide <https://github.com/excilys/gatling/wiki/Migrating#wiki-1.2.0>`__

Features
~~~~~~~~

-  `#376 <https://github.com/excilys/gatling/issues/376>`__ loop times
   condition can now be dynamic
-  `#432 <https://github.com/excilys/gatling/issues/432>`__ &
   `#523 <https://github.com/excilys/gatling/issues/523>`__ Referer
   header can now be automatically computed
-  `#435 <https://github.com/excilys/gatling/issues/435>`__ &
   `#518 <https://github.com/excilys/gatling/pull/518>`__ CSS Selector
   extractors are now supported, thanks to Nicolas Rémond (@nire)
-  `#493 <https://github.com/excilys/gatling/issues/493>`__ &
   `#531 <https://github.com/excilys/gatling/pull/531>`__ HEAD HTTP word
   is now supported, thanks to Nicolas Rémond (@nire)
-  `#501 <https://github.com/excilys/gatling/issues/501>`__ Reports
   global page has been refactored
-  `#509 <https://github.com/excilys/gatling/pull/509>`__ Recorder has
   been migrated to Scala
-  `#514 <https://github.com/excilys/gatling/issues/514>`__ Common HTTP
   Headers can be configured on ProtocolConfiguration
-  `#522 <https://github.com/excilys/gatling/issues/522>`__ Outgoing
   proxy credentials can now be configured in the Recorder
-  `#527 <https://github.com/excilys/gatling/issues/527>`__ Percentiles
   have been reworked to make more sense
-  `#530 <https://github.com/excilys/gatling/pull/530>`__ New
   exponentially distributed pauses, thanks to Stephen Kuenzli
   (@skuenzli)
-  `#532 <https://github.com/excilys/gatling/issues/532>`__ Add
   automatic request to compensate for engine warm up
-  `#535 <https://github.com/excilys/gatling/issues/535>`__ Calling
   check() several times will now append them instead of overriding them

Fixes
~~~~~

-  `#512 <https://github.com/excilys/gatling/pull/512>`__ &
   `#528 <https://github.com/excilys/gatling/pull/528>`__ Fix class name
   resolution in gatling-maven-plugin, thanks to Cyril Couturi
   (@ccouturi) and Stephen Kuenzli (@skuenzli)
-  `#520 <https://github.com/excilys/gatling/issues/520>`__ Add
   protection from SimpleAction crashes
-  `#534 <https://github.com/excilys/gatling/issues/534>`__ Handle empty
   lines in CSV files

`v1.1.6 <https://github.com/excilys/gatling/issues?milestone=12&state=closed>`__ - Bug fix
--------------------------------------------------------------------------------------------

Fixes
~~~~~

-  `#498 <https://github.com/excilys/gatling/issues/498>`__ Recorder:
   fix NPE on request body
-  `#507 <https://github.com/excilys/gatling/issues/507>`__
   gatling-maven-plugin: fix simulation package on Windows
-  `#508 <https://github.com/excilys/gatling/issues/508>`__ Charts: fix
   encoding
-  `#510 <https://github.com/excilys/gatling/issues/510>`__ Recorder:
   fix request bodies folder name

`v1.1.5 <https://github.com/excilys/gatling/issues?milestone=11&state=closed>`__ - Bug fix
--------------------------------------------------------------------------------------------

Fixes
~~~~~

-  `#489 <https://github.com/excilys/gatling/issues/489>`__ Make
   recorder use relative URIs once connection established
-  `#490 <https://github.com/excilys/gatling/issues/490>`__ Handle 303
   redirect status code
-  `#491 <https://github.com/excilys/gatling/issues/491>`__ Fix status
   code check when using non default one
-  `#497 <https://github.com/excilys/gatling/issues/497>`__ Fix reports
   when request name contains "'"
-  `#498 <https://github.com/excilys/gatling/issues/498>`__ Fix NPE in
   recorder when dumping request bodies
-  `#499 <https://github.com/excilys/gatling/issues/499>`__ Fix latency
   chart

Features
~~~~~~~~

-  `#484 <https://github.com/excilys/gatling/issues/484>`__ - Remove
   check logic from the AHC handler in order to reduce pressure on IO
   threads
-  `#486 <https://github.com/excilys/gatling/issues/486>`__ - Charts:
   all session series is computed once for all
-  `#492 <https://github.com/excilys/gatling/issues/492>`__ - Add a
   "maybe" check strategy when one want to capture an optional value
-  `#500 <https://github.com/excilys/gatling/issues/450>`__ - Document
   transactions/sec chart
-  `#502 <https://github.com/excilys/gatling/issues/502>`__ - Expose AHC
   configuration in Gatling conf

`v1.1.4 <https://github.com/excilys/gatling/issues?milestone=10&state=closed>`__ - Bug fix
--------------------------------------------------------------------------------------------

Fixes
~~~~~

-  `#481 <https://github.com/excilys/gatling/issues/481>`__ Fix http
   client start up
-  `#483 <https://github.com/excilys/gatling/issues/483>`__ Fix multiple
   simulations launching

Features
~~~~~~~~

-  `#485 <https://github.com/excilys/gatling/issues/485>`__ - Charts:
   add new response time distribution
-  `#487 <https://github.com/excilys/gatling/issues/487>`__ - EL: let
   occurrence be dynamic

`v1.1.3 <https://github.com/excilys/gatling/issues?milestone=9&state=closed>`__ - Bug fix
-------------------------------------------------------------------------------------------

Fixes
~~~~~

-  `#459 <https://github.com/excilys/gatling/issues/459>`__ - Upgrade
   Netty 3.4.0.Final that fixes a compression bug.
-  `#460 <https://github.com/excilys/gatling/issues/460>`__ - Fix
   recorder SSL certificate.
-  `#466 <https://github.com/excilys/gatling/issues/466>`__ - Support
   relative Location headers
-  `#469 <https://github.com/excilys/gatling/issues/469>`__ -
   Regression: the recorder shouldn't record Cookie and Content-Length
   headers
-  `#470 <https://github.com/excilys/gatling/issues/470>`__ - Fix
   statistics displayed in the CLI while running

Features
~~~~~~~~

-  `#465 <https://github.com/excilys/gatling/issues/465>`__ - Charts:
   display percentiles

`v1.1.2 <https://github.com/excilys/gatling/issues?milestone=8&state=closed>`__ - Bug fix
-------------------------------------------------------------------------------------------

Fixes
~~~~~

-  `#450 <https://github.com/excilys/gatling/issues/450>`__ - Properly
   fixes cookie expiration
-  `#453 <https://github.com/excilys/gatling/issues/453>`__ - Make
   XPathExtractor threadsafe
-  `#455 <https://github.com/excilys/gatling/issues/455>`__ - Fix global
   statistics

Features
~~~~~~~~

-  `#327 <https://github.com/excilys/gatling/issues/327>`__ - Akka 2
   migration, wouhou!!!

`v1.1.1 <https://github.com/excilys/gatling/issues?milestone=7&state=closed>`__ - Bug fix
-------------------------------------------------------------------------------------------

Fixes
~~~~~

-  `#442 <https://github.com/excilys/gatling/issues/442>`__ - Fixes
   fileBody templating
-  `#444 <https://github.com/excilys/gatling/issues/444>`__ - Fixes
   cookie deletion

Features
~~~~~~~~

-  `#447 <https://github.com/excilys/gatling/issues/447>`__ - Log at
   debug level the response on failed check

`v1.1.0 <https://github.com/excilys/gatling/issues?milestone=2&sort=created&direction=desc&state=closed>`__
-------------------------------------------------------------------------------------------------------------

    Warning: Migration required, see `migration
    guide <https://github.com/excilys/gatling/wiki/Migrating#wiki-1.1.0>`__

Features
~~~~~~~~

-  Engine
-  configurable run id and run description, see
   `#416 <https://github.com/excilys/gatling/issues/416>`__
-  periodic statistic display while running, see
   `#384 <https://github.com/excilys/gatling/issues/384>`__
-  link to generated reports, see
   `#383 <https://github.com/excilys/gatling/issues/383>`__
-  Check API
-  Check API is now type safe
-  optional transform step on extraction result
-  new JSONPath, see
   `#433 <https://github.com/excilys/gatling/issues/433>`__
-  xpath namespaces support, see
   `#434 <https://github.com/excilys/gatling/issues/434>`__
-  Feeder API
-  new JDBC feeders for retrieving data from a SGBDR, see
   `#37 <https://github.com/excilys/gatling/issues/37>`__
-  escape character support on CSV based feeders, see
   `#105 <https://github.com/excilys/gatling/issues/105>`__
-  circular feeders, see
   `#321 <https://github.com/excilys/gatling/issues/321>`__
-  HTTP API
-  follow redirect support, see
   `#105 <https://github.com/excilys/gatling/issues/105>`__
-  clean cookie handling, see
   `#396 <https://github.com/excilys/gatling/issues/396>`__
-  Charts API
-  configurable time window, see
   `#323 <https://github.com/excilys/gatling/issues/323>`__
-  new active transactions/sec over time chart
-  new response latency over time chart
-  Recorder
-  no longer an ubber jar, now to be launched from a script
-  follow redirect support
-  configurable generated Simulation package and class name, see
   `#438 <https://github.com/excilys/gatling/issues/438>`__
-  configurable encoding, see
   `#386 <https://github.com/excilys/gatling/issues/386>`__
-  Extensions
-  new gatling-maven-plugin, contributed by @nhuray
-  new gatling debian package, contributed by @nhuray

And tons of bug fixes and performance enhancements!

`v1.0.3 <https://github.com/excilys/gatling/issues?sort=created&direction=desc&state=closed&page=1&milestone=5>`__ - Bug fix
------------------------------------------------------------------------------------------------------------------------------

Fixes
~~~~~

-  Fix a bug in the recorder introduced in 1.0.2 that prevent from
   recording scenarios with less than 100 requests

`v1.0.2 <https://github.com/excilys/gatling/issues?sort=created&direction=desc&state=closed&page=1&milestone=4>`__ - Bug fix
------------------------------------------------------------------------------------------------------------------------------

Features
~~~~~~~~

-  `#345 <https://github.com/excilys/gatling/issues/345>`__,
   `#348 <https://github.com/excilys/gatling/issues/348>`__ &
   `#330 <https://github.com/excilys/gatling/issues/330>`__ - Better
   support for long scenarios (via `manual
   splitting <https://github.com/excilys/gatling/wiki/First-Steps-with-Gatling#wiki-long-scenarios>`__)
-  `#347 <https://github.com/excilys/gatling/issues/334>`__ - Recorder
   splits long scenarios so they can be run with no extra configuration.

`v1.0.1 <https://github.com/excilys/gatling/issues?sort=created&direction=desc&state=closed&page=1&milestone=3>`__ - Bug fix
------------------------------------------------------------------------------------------------------------------------------

Fixes
~~~~~

-  `#334 <https://github.com/excilys/gatling/issues/334>`__ - Fixes
   reports template resolution under Windows
-  `#320 <https://github.com/excilys/gatling/issues/320>`__ - Stops
   scenario if queue feeder not big enough
-  Fixes a bug with empty lines at end of feeders

Features
~~~~~~~~

-  Better CLI feedback

`v1.0.0 <https://github.com/excilys/gatling/issues?milestone=1&state=closed>`__ - Initial Release
---------------------------------------------------------------------------------------------------

