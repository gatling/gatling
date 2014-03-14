############
Maven Plugin
############

.. highlight:: xml

Thanks to this plugin, Gatling can be launched when building your project, for example with your favorite CI solution.

Set up the repositories
=======================

Gatling depends on libraries that are either hosted on maven central, or on Excilys Nexus.

Depending on your maven architecture, you might have to:

* add the Excilys repository into your project's pom.xml if you're fine with downloading your dependencies from the internet. In this case, here's the required configuration::

	<repositories>
	    <repository>
	        <id>excilys</id>
	        <name>Excilys Repository</name>
	        <url>http://repository.excilys.com/content/groups/public</url>
	    </repository>
	</repositories>

	<pluginRepositories>
	    <pluginRepository>
	        <id>excilys</id>
	        <name>Excilys Repository</name>
	        <url>http://repository.excilys.com/content/groups/public</url>
	    </pluginRepository>
	</pluginRepositories>

* add a proxy on Excilys repository into your enterprise maven repository (Nexus, Artifactory, Archiva...)
* manually download and install the dependencies into your enterprise repository if you can't/don't want to add a proxy

Set up the gatling-maven-plugin
===============================

::

	<dependencies>
	    <dependency>
	        <groupId>com.excilys.ebi.gatling.highcharts</groupId>
	        <artifactId>gatling-charts-highcharts</artifactId>
	        <version>X.Y.Z</version>
	        <scope>test</scope>
	    </dependency>
	<dependencies>

	<plugin>
	    <groupId>com.excilys.ebi.gatling</groupId>
	    <artifactId>gatling-maven-plugin</artifactId>
	    <version>X.Y.Z</version>
	</plugin>

.. note:: As the Highcharts based reports library is developed as a separate project, you are required to provide it as a dependency.

.. _maven-advanced-configuration:

Optional advanced configuration
===============================

The example below shows the default values.

::

	<configuration>
	    <configDir>src/test/resources</configDir>
	    <dataFolder>src/test/resources/data</dataFolder>
	    <resultsFolder>target/gatling/results</resultsFolder>
	    <requestBodiesFolder>src/test/resources/request-bodies</requestBodiesFolder>
	    <simulationsFolder>src/test/scala</simulationsFolder>
	    <includes>
	        <include>**/*.scala</include>
	    </includes>
	    <excludes>
	        <exclude>advanced/*.scala</exclude>
	    </excludes>
	<!--    <noReports>false</noReports> -->
	<!-- 	<reportsOnly>false</reportsOnly> -->
	<!-- 	<simulationClass>foo.Bar</simulationClass> -->
	<!-- 	<jvmArgs> -->
	<!-- 	    <jvmArg>-DmyExtraParam=foo</jvmArg> -->
	<!-- 	</jvmArgs> -->
	<!--    <fork>true</fork> -->
	<!--    <propagateSystemProperties>true</propagateSystemProperties> -->
	<!-- 	<failOnError>true</failOnError> -->
	</configuration>

See `source code <https://github.com/excilys/gatling/blob/1.5.X/gatling-maven-plugin/src/main/java/com/excilys/ebi/gatling/mojo/GatlingMojo.java>`_ for more documentation. 

Override the logback.xml file
=============================

You can either have a ``logback-test.xml`` that has precedence over the embedded ``logback.xml`` file, or add a JVM option ``-Dlogback.configurationFile=myFilePath``.

Sample
======

See sample project `here <https://github.com/excilys/gatling-maven-plugin-demo>`_.