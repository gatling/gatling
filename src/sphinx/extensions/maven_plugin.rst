.. _maven-plugin:

############
Maven Plugin
############

.. highlight:: xml

Thanks to this plugin, Gatling can be launched when building your project, for example with your favorite CI solution.

Versions
========

Check out available versions on `Maven Central <http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22io.gatling%22%20AND%20a%3A%22gatling-maven-plugin%22>`_.

Beware that milestones (M versions) are undocumented and releases for Gatling customers.


Set up the gatling-maven-plugin
===============================

::

  <dependencies>
    <dependency>
      <groupId>io.gatling.highcharts</groupId>
      <artifactId>gatling-charts-highcharts</artifactId>
      <version>X.Y.Z</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <plugin>
    <groupId>io.gatling</groupId>
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
    <configFolder>${project.basedir}/src/test/resources</configFolder>
    <dataFolder>${project.basedir}/src/test/resources/data</dataFolder>
    <resultsFolder>${project.basedir}/target/gatling/results</resultsFolder>
    <bodiesFolder>${project.basedir}/src/test/resources/bodies</bodiesFolder>
    <simulationsFolder>${project.basedir}/src/test/scala</simulationsFolder>
  <!--    <noReports>false</noReports> -->
  <!--   <reportsOnly>directoryName</reportsOnly> -->
  <!--   <simulationClass>foo.Bar</simulationClass> -->
  <!--   <jvmArgs> -->
  <!--     <jvmArg>-DmyExtraParam=foo</jvmArg> -->
  <!--   </jvmArgs> -->
  <!--    <fork>true</fork> -->
  <!--    <propagateSystemProperties>true</propagateSystemProperties> -->
  <!--   <failOnError>true</failOnError> -->
  </configuration>

See `source code <https://github.com/gatling/gatling-maven-plugin/blob/master/src/main/java/io/gatling/mojo/GatlingMojo.java>`_ for more documentation.

Override the logback.xml file
=============================

You can either have a ``logback-test.xml`` that has precedence over the embedded ``logback.xml`` file, or add a JVM option ``-Dlogback.configurationFile=myFilePath``.

Running the Plugin
==================

You can directly launch the gatling-maven-plugin with the ``test`` or ``integration-test`` task::

  mvn gatling:test             // bound to test phase
  mvn gatling:integration-test // bound to integration-test phase

Then, you probably want to have it attached to a maven lifecycle phase so it's automatically triggered.
You then have to configure an `execution <http://maven.apache.org/guides/mini/guide-configuring-plugins.html#Using_the_executions_Tag>`_ block.

::

  <plugin>
    <groupId>io.gatling</groupId>
    <artifactId>gatling-maven-plugin</artifactId>
    <version>${gatling.version}</version>
    <!-- optional if you only have one simulation -->
    <configuration>
      <simulationClass>Foo</simulationClass>
    </configuration>
    <executions>
      <execution>
        <goals>
          <goal>integration-test</goal>
        </goals>
      </execution>
    </executions>
  </plugin>

Then, you may want to run the plugin several times in a build (e.g. in order to run several Simulations sequentially).
A solution is to configure several ``execution``s with each having a different ``configuration`` block.
If you do so, beware that those won't be used when running ``gatling:test``, as executions are triggered by maven phases.

::

  <plugin>
    <groupId>io.gatling</groupId>
    <artifactId>gatling-maven-plugin</artifactId>
    <version>${gatling.version}</version>
    <executions>
      <execution>
        <id>execution1</id>
        <goals>
          <goal>integration-test</goal>
        </goals>
        <configuration>
          <simulationClass>Foo</simulationClass>
        </configuration>
      </execution>
      <execution>
        <id>execution2</id>
        <goals>
          <goal>integration-test</goal>
        </goals>
        <configuration>
          <simulationClass>Bar</simulationClass>
        </configuration>
      </execution>
    </executions>
  </plugin>

Sample
======

See sample project `here <https://github.com/gatling/gatling-maven-plugin-demo>`_.
