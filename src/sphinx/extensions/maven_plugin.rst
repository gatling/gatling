.. _maven-plugin:

############
Maven Plugin
############

.. highlight:: xml

Thanks to this plugin, Gatling can be launched when building your project, for example with your favorite CI solution.

Versions
========

Check out available versions on `Maven Central <http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22io.gatling%22%20AND%20a%3A%22gatling-maven-plugin%22>`_.

Beware that milestones (M versions) are undocumented and released for Gatling customers.


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
    <runDescription>This-is-the-run-description</runDescription>
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

Please check `source code <https://github.com/gatling/gatling-maven/blob/master/gatling-maven-plugin/src/main/java/io/gatling/mojo/GatlingMojo.java>`_ for all,possible options.

Including / excluding simulations when running multiple simulations
-------------------------------------------------------------------
If you would like to run multiple simulations you can use the following option 

::

  <configuration>
    <!--   ...  -->
    <runMultipleSimulations>true</runMultipleSimulations>
    <!--   ...  -->
  </configuration>
  
In conjonction of that option you can use the ``includes`` and ``excludes`` filter options. ``includes`` will act as a `whitelist <https://en.wikipedia.org/wiki/Whitelist>`_.

::

  <configuration>
    <!--   ...  -->
    <runMultipleSimulations>true</runMultipleSimulations>
    <includes>
      <param>my.package.MySimu1</param>
      <param>my.package.MySimu2</param>
    </includes>
  </configuration>

.. note:: The order of parameters does not correspond to the execution order. You can use multiple executions to force an order between your simulations (see last section of this page).

``excludes`` acts as a `blacklist <https://en.wikipedia.org/wiki/Blacklisting>`_.

::

  <configuration>
    <!--   ...  -->
    <runMultipleSimulations>true</runMultipleSimulations>
    <excludes>
      <param>my.package.MySimuNotToRun</param>
    </excludes>
  </configuration>
  
Coexisting with scala-maven-plugin
==================================

If you decide to turn your maven project into a full blown Scala and use the `scala-maven-plugin <https://github.com/davidB/scala-maven-plugin>`_,
depending on how you run your maven tasks, you might end up compiling your simulations twice: once by the scala-maven-plugin, and once by the gatling-maven-plugin.

If so, you can disable the gatling-maven-plugin compiling phase::

  <configuration>
    <disableCompiler>true</disableCompiler>
  </configuration>


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
A solution is to configure several ``execution`` blocks with each having a different ``configuration`` block.
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
