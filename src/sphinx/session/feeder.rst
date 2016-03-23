.. _feeder:

#######
Feeders
#######

Feeder is a type alias for ``Iterator[Map[String, T]]``, meaning that the component created by the feed method will poll ``Map[String, T]`` records and inject its content.

It's very simple to build a custom one. For example, here's how one could build a random email generator:

.. includecode:: code/Feeders.scala#random-mail-generator

The structure DSL provides a ``feed`` method.

.. includecode:: code/Feeders.scala#feed

This defines a workflow step where **every virtual user** feed on the same Feeder.

Every time a virtual user reaches this step, it will pop a record out of the Feeder, which will be injected into the user's Session, resulting in a new Session instance.

If the Feeder can't produce enough records, Gatling will complain about it and your simulation will stop.

.. note::
  You can also feed multiple records all at once. If so, attribute names, will be suffixed.
  For example, if the columns are name "foo" and "bar" and you're feeding 2 records at once, you'll get "foo1", "bar1", "foo2" and "bar2" session attributes.

.. includecode:: code/Feeders.scala#feed-multiple

.. _feeder-builder:

RecordSeqFeederBuilder
======================

An ``Array[Map[String, T]]`` or a ``IndexedSeq[Map[String, T]]`` can be implicitly turned into a Feeder.
Moreover, this implicit conversion also provides some additional methods for defining the way the Seq is iterated over:

.. includecode:: code/Feeders.scala#strategies

For example:

.. includecode:: code/Feeders.scala#feeder-from-array-with-random

.. _feeder-csv:

CSV feeders
===========

Gatling provides several builtins for reading character-separated values files.

Files are expected to be placed in the ``data`` directory in Gatling distribution. This location can be overridden, see :ref:`configuration`.

By default, our parser respects `RFC4180 <https://tools.ietf.org/html/rfc4180>`_, so don't expect behaviors that don't honor this specification.

The only difference is that header fields get trimmed of wrapping whitespaces.

.. includecode:: code/Feeders.scala#sep-values-feeders

Those built-ins returns ``RecordSeqFeederBuilder`` instances, meaning that the whole file is loaded in memory and parsed, so the resulting feeders doesn't read on disk during the simulation run.

.. warning::
  Loading feeder files in memory uses a lot of heap, expect a 5-to-10-times ratio with the file size.
  This is due to JVM's internal UTF-16 char encoding and object headers overhead.
  If memory is an issue for you, you might want to read from the filesystem on the fly and build your own Feeder.

Besides quoting feature described in the RFC, one can specify an escape character so some content characters don't get confused for separator or quoting ones.

.. includecode:: code/Feeders.scala#escape-char

.. _feeder-json:

JSON feeders
============

Some might want to use data in JSON format instead of CSV:

.. includecode:: code/Feeders.scala#json-feeders

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

.. includecode:: code/Feeders.scala#jdbc-feeder

Just like File parser built-ins, this return a ``RecordSeqFeederBuilder`` instance.

* The databaseURL must be a JDBC URL (e.g. ``jdbc:postgresql:gatling``),
* the username and password are the credentials to access the database,
* sql is the query that will get the values needed.

Only JDBC4 drivers are supported, so that they automatically registers to the DriverManager.

.. note::
    Do not forget to add the required JDBC driver jar in the classpath (``lib`` folder in the bundle)

.. _feeder-redis:

Sitemap Feeder
==============

Gatling supports a feeder that reads data from a `Sitemap <http://www.sitemaps.org/protocol.html>`_ file.

.. includecode:: code/Feeders.scala#sitemap-feeder

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

Redis feeder
============

This feature was originally contributed by Krishnen Chedambarum.

Gatling can read data from Redis using one of the following Redis commands.

* LPOP - remove and return the first element of the list
* SPOP - remove and return a random element from the set
* SRANDMEMBER - return a random element from the set

By default RedisFeeder uses LPOP command:

.. includecode:: code/Feeders.scala#redis-LPOP

An optional third parameter is used to specify desired Redis command:

.. includecode:: code/Feeders.scala#redis-SPOP

Note that since v2.1.14, Redis supports mass insertion of data from a `file <http://redis.io/topics/mass-insert>`_.
It is possible to load millions of keys in a few seconds in Redis and Gatling will read them off memory directly.

For example: a simple Scala function to generate a file with 1 million different urls ready to be loaded in a Redis list named *URLS*:

.. includecode:: code/Feeders.scala#redis-1million

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

.. includecode:: code/Feeders.scala#convert

.. _feeder-non-shared:

Non Shared Data
===============

Sometimes, you could want all virtual users to play all the records in a file, and Feeder doesn't match this behavior.

Still, it's quite easy to build, thanks to :ref:`flattenMapIntoAttributes <scenario-exec-function-flatten>`  e.g.:

.. includecode:: code/Feeders.scala#non-shared

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

.. includecode:: code/Feeders.scala#user-dependent-data
