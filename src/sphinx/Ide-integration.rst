***************
Ide integration
***************

To help Scala developers with the creation of Gatling simulations in
Scala, we provide a Maven Archetype that will create a project with
every dependencies set, and the ability to start Gatling Engine and
Gatling Recorder from the IDE.

Using this archetype to create your simulations allows you to use
autocompletion while writing the simulations.

Prerequisites
-------------

You need a Scala development environment.

Eclipse users might want to have a look at the Gatling [[Development
environment]] regarding m2e and m2e-scala installation instructions.

    Beware that the generated classes might not behave properly if your
    workspace's path contains spaces, like the dreaded "My Documents" on
    Windows.

Installing the Maven Archetype
------------------------------

Here are the Gatling Maven Archetype information:

::

    Catalog:
    http://repository.excilys.com/content/groups/public/archetype-catalog.xml

.. code:: xml

        <groupId>com.excilys.ebi.gatling.highcharts</groupId>
        <artifactId>gatling-highcharts-maven-archetype</artifactId>

    Note: If you are using an Enterprise maven repository that acts as a
    proxy for other repositories on the internet, you'll have to set up
    a proxy for the following repository:

.. code:: xml

    <repository>
        <id>excilys</id>
        <name>Excilys Repository</name>
        <url>http://repository.excilys.com/content/groups/public</url>
    </repository>

This repository hosts Gatling binaries and all the required dependencies
that are not hosted on maven central.

Running the Recorder
--------------------

Simply launch the ``Recorder`` Scala class located in the default
package.

Simulations are generated in the ``src/test/scala`` directory (you might
have to refresh your project).

Running the Engine
------------------

Simply launch the ``Engine`` Scala class located in the default package.

Results are generated in the ``target`` directory.
