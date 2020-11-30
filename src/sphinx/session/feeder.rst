.. _feeder:

#######
Feeders
#######

Feeder is a type alias for ``Iterator[Map[String, T]]``, meaning that the component created by the feed method will poll ``Map[String, T]`` records and inject its content.

It's very simple to build a custom one. For example, here's how one could build a random email generator:

.. includecode:: code/FeederSample.scala#random-mail-generator

The structure DSL provides a ``feed`` method.

.. includecode:: code/FeederSample.scala#feed

This defines a workflow step where **every virtual user** feed on the same Feeder.

Every time a virtual user reaches this step, it will pop a record out of the Feeder, which will be injected into the user's Session, resulting in a new Session instance.

If the Feeder can't produce enough records, Gatling will complain about it and your simulation will stop.

.. note::
  You can also feed multiple records all at once. If so, attribute names, will be suffixed.
  For example, if the columns are name "foo" and "bar" and you're feeding 2 records at once, you'll get "foo1", "bar1", "foo2" and "bar2" session attributes.

.. includecode:: code/FeederSample.scala#feed-multiple

.. _feeder-strategy:

Strategies
==========

Gatling provides multiple strategies for the built-in feeders:

.. includecode:: code/FeederSample.scala#strategies

.. warning::
  When using the default ``queue`` strategy, make sure that your dataset contains enough records.
  If your feeder runs out of record, behavior is undefined and Gatling will forcefully shut down.

.. _feeder-implicits:

Implicits
=========

An ``Array[Map[String, T]]`` or a ``IndexedSeq[Map[String, T]]`` can be implicitly turned into a Feeder.
For example:

.. includecode:: code/FeederSample.scala#feeder-from-array-with-random

.. _feeder-files:

File Based Feeders
==================

Gatling provides various file based feeders.

When using the bundle distribution, files must be in the ``user-files/resources`` directory. This location can be overridden, see :ref:`configuration`.

When using a build tool such as maven, files must be placed in ``src/main/resources`` or ``src/test/resources``.

In order the locate the file, Gatling try the following strategies in sequence:

1. as a classpath resource from the classpath root, eg ``data/file.csv`` for targeting the ``src/main/resources/data/file.csv`` file. This strategy is the recommended one.
2. from the filesystem, as a path relative to the Gatling root dir. This strategy should only be used when using the Gatling bundle.
3. from the filesystem, as an absolute path. Use this strategy if you want your feeder files to be deployed separately.

.. warning::
Do NOT rely on having an exploded gradle/maven/sbt project structure.
Typically, don't use strategy #2 and paths such as ``src/main/resources/data/file.csv``.
The exploded structure might no longer be there at runtime, all the more when deploying with `FrontLine <https://gatling.io/gatling-frontline/>`_.
Use strategy #1 and classpath paths such as ``data/file.csv``.

.. _feeder-csv:

CSV feeders
===========

Gatling provides several built-ins for reading character-separated values files.

Our parser honors the `RFC4180 <https://tools.ietf.org/html/rfc4180>`_ specification.

The only difference is that header fields get trimmed of wrapping whitespaces.

.. includecode:: code/FeederSample.scala#sep-values-feeders

.. _feeder-loading-mode:

Loading mode
============

CSV files feeders provide several options for how data should be loaded in memory.

``eager`` loads the whole data in memory before the Simulation starts, saving disk access at runtime.
This mode works best with reasonably small files that can be parsed quickly without delaying simulation start time and easily sit in memory.
This behavior was the default prior to Gatling 3.1 and you can still force it.

.. includecode:: code/FeederSample.scala#eager

``batch`` works better with large files whose parsing would delay simulation start time and eat a lot of heap space.
Data is then read by chunks.

.. warning::
  When in ``batch`` mode, ``random`` and ``shuffle`` can't of course operate on the full stock, and only operate on an internal buffer of records.
  The default size of this buffer is 2,000 and can be changed.

.. includecode:: code/FeederSample.scala#batch

Default behavior is an adaptive policy based on (unzipped, sharded) file size, see ``gatling.core.feederAdaptiveLoadModeThreshold`` in config file.
Gatling will use ``eager`` below threshold and `batch` above.

.. _feeder-unzip:

Zipped files
============

If your files are very large, you can provide them zipped and ask gatling to ``unzip`` them on the fly:

.. includecode:: code/FeederSample.scala#unzip

Supported formats are gzip and zip (but archive most contain only one single file).

.. _feeder-shard:

Distributed files (FrontLine only)
==================================

If you want to run distributed with `FrontLine <https://gatling.io/gatling-frontline/>`_
and you want to distribute data so that users don't use the same data when they run on different cluster nodes, you can use the ``shard`` option.
For example, if you have a file with 30,000 records deployed on 3 nodes, each will use a 10,000 records slice.

.. warning::
``shard`` is only effective when running with FrontLine, otherwise it's just a noop.

.. includecode:: code/FeederSample.scala#shard

.. _feeder-json:

JSON feeders
============

Some might want to use data in JSON format instead of CSV:

.. includecode:: code/FeederSample.scala#json-feeders

For example, the following JSON::

  [
    {
      "id":19434,
      "foo":1
    },
    {
      "id":19435,
      "foo":2
    }
  ]

will be turned into::

  record1: Map("id" -> 19434, "foo" -> 1)
  record2: Map("id" -> 19435, "foo" -> 2)


Note that the root element has of course to be an array.

.. _feeder-jdbc:

JDBC feeder
===========

Gatling also provide a builtin that reads from a JDBC connection.

.. includecode:: code/FeederSample.scala#jdbc-feeder

Just like File parser built-ins, this return a ``RecordSeqFeederBuilder`` instance.

* The databaseUrl must be a JDBC URL (e.g. ``jdbc:postgresql:gatling``),
* the username and password are the credentials to access the database,
* sql is the query that will get the values needed.

Only JDBC4 drivers are supported, so that they automatically registers to the DriverManager.

.. note::
    Do not forget to add the required JDBC driver jar in the classpath (``lib`` folder in the bundle)

.. _feeder-sitemap:

Sitemap Feeder
==============

Gatling supports a feeder that reads data from a `Sitemap <http://www.sitemaps.org/protocol.html>`_ file.

.. includecode:: code/FeederSample.scala#sitemap-feeder

The following Sitemap file::

  <?xml version="1.0" encoding="UTF-8"?>
  <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
    <url>
      <loc>http://www.example.com/</loc>
      <lastmod>2005-01-01</lastmod>
      <changefreq>monthly</changefreq>
      <priority>0.8</priority>
    </url>

    <url>
      <loc>http://www.example.com/catalog?item=12&amp;desc=vacation_hawaii</loc>
      <changefreq>weekly</changefreq>
    </url>

    <url>
      <loc>http://www.example.com/catalog?item=73&amp;desc=vacation_new_zealand</loc>
      <lastmod>2004-12-23</lastmod>
      <changefreq>weekly</changefreq>
    </url>
  </urlset>

will be turned into::

  record1: Map(
             "loc" -> "http://www.example.com/",
             "lastmod" -> "2005-01-01",
             "changefreq" -> "monthly",
             "priority" -> "0.8")
          
  record2: Map(
             "loc" -> "http://www.example.com/catalog?item=12&amp;desc=vacation_hawaii",
             "changefreq" -> "weekly")

  record3: Map(
             "loc" -> "http://www.example.com/catalog?item=73&amp;desc=vacation_new_zealand",
             "lastmod" -> "2004-12-23",
             "changefreq" -> "weekly")

.. _feeder-redis:

Redis feeder
============

This feature was originally contributed by Krishnen Chedambarum.

Gatling can read data from Redis using one of the following Redis commands.

* LPOP - remove and return the first element of the list
* SPOP - remove and return a random element from the set
* SRANDMEMBER - return a random element from the set

By default RedisFeeder uses LPOP command:

.. includecode:: code/FeederSample.scala#redis-LPOP

You can then override the desired Redis command:

.. includecode:: code/FeederSample.scala#redis-SPOP

.. includecode:: code/FeederSample.scala#redis-SRANDMEMBER

Note that since v2.1.14, Redis supports mass insertion of data from a `file <https://redis.io/topics/mass-insert>`_.
It is possible to load millions of keys in a few seconds in Redis and Gatling will read them off memory directly.

For example: a simple Scala function to generate a file with 1 million different urls ready to be loaded in a Redis list named *URLS*:

.. includecode:: code/FeederSample.scala#redis-1million

The urls can then be loaded in Redis using the following command::

  `cat /tmp/loadtest.txt | redis-cli --pipe`

.. _feeder-convert:

Converting
==========

Sometimes, you might want to convert the raw data you got from your feeder.

For example, a csv feeder would give you only Strings, but you might want to convert one of the attribute into an Int.

``convert(conversion: PartialFunction[(String, T), Any])`` takes:

* a PartialFunction, meaning that you only define it for the scope you want to convert, non matching attributes will be left unchanged
* whose input is a (String, T) couple where the first element is the attribute name, and the second one the attribute value
* and whose output is Any, whatever you want

For example:

.. includecode:: code/FeederSample.scala#convert

.. _feeder-records:

Grabbing Records
================

Sometimes, you just might want to reuse or convenient built-in feeders for custom needs and get your hands on the actual records.

``readRecords`` returns a ``Seq[Map[String, Any]]``.

.. includecode:: code/FeederSample.scala#records

.. warning::
  Beware that each ``readRecords`` call will read the underlying source, eg parse the CSV file.

.. _feeder-non-shared:

Non Shared Data
===============

Sometimes, you could want all virtual users to play all the records in a file, and Feeder doesn't match this behavior.

Still, it's quite easy to build, thanks to :ref:`flattenMapIntoAttributes <scenario-exec-function-flatten>`  e.g.:

.. includecode:: code/FeederSample.scala#non-shared

.. _feeder-user-dependent:

User Dependent Data
===================

Sometimes, you could want to filter the injected data depending on some information from the Session.

Feeder can't achieve this as it's just an Iterator, so it's unaware of the context.

You'll then have to write your own injection logic, but you can of course reuse Gatling parsers.

Consider the following example, where you have 2 files and want to inject data from the second one,
depending on what has been injected from the first one.

In userProject.csv::

  user, project
  bob, aProject
  sue, bProject

In projectIssue.csv::

  project,issue
  aProject,1
  aProject,12
  aProject,14
  aProject,15
  aProject,17
  aProject,5
  aProject,7
  bProject,1
  bProject,2
  bProject,6
  bProject,64

Here's how you can randomly inject an issue, depending on the project:

.. includecode:: code/FeederSample.scala#user-dependent-data
