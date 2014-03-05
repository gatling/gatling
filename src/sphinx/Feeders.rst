*******
Feeders
*******

This page references all the feeders available in Gatling.

Understanding Feeders
---------------------

Why use feeders?
~~~~~~~~~~~~~~~~

If you want to simulate 100 users, each with its own credentials for
example, you need to inject data in the simulation for each user.
Another similar use case could be requests consuming data to simulate
the creation of records in the database via the tested application (new
products, carts, etc.)

What feeders are exactly
~~~~~~~~~~~~~~~~~~~~~~~~

A Feeder is an object **shared amongst users** that will inject data
into a given user's Session every time it reaches a feed method call.

Technically speaking, it's an ``Iterator[Map[String, T]]``, meaning that the
component created by the feed method will poll ``Map[String, T]`` records
and inject its content.

Depending on where you place the ``feed`` method, the data injected in
the session won't be the same. Imagine the following scenario extract:

.. code:: scala

    val userCredentials = csv("user_credentials.csv").queue
    val recordsInformation = csv("records_information.csv").queue

    val scn = scenario("My Scenario")
      .feed(userCredentials)
      .repeat(20) {
          feed(recordsInformation)
          .exec( ...)
      }
      // End of Scenario
      
    setUp(scn.users(100))

At the ``End of Scenario`` line, the data contained in the session for
one user is half predictible:

-  The data from the ``userCredentials`` feeder will match the userId of
   the session (the first launched scenario will get the first value
   from the file and so on)
-  The data from the ``recordsInformation`` feeder could be anything.
   Indeed, each user will get values from the feeder simultaneously
   (because of the loop). Therefore, one cannot predict what records
   will be created by which user.

  Note: If the first ``feed`` method was called after the loop, there
  would be no guarantees that the 20th user would have taken the 20th
  line of the feeder.

Data Sources
------------

Data Sources are basically ``Array[Map[String, T]]``.

Map keys are the names of the attributes once a record has been pushed
in a user's Session.

File Parsers
~~~~~~~~~~~~

Gatling provide support for separated values files. There are three
types of separated values files supported:

-  Comma Separated Values (CSV).
-  Semicolon Separated Values (SSV).
-  Tabulation Separated Values (TSV).

To parse a file with a separated values file as source, use the
following methods:

.. code:: scala

    csv( filename: String ) // If the values are _comma_ separated
    ssv( filename: String ) // If the values are _semi colon_ separated
    tsv( filename: String ) // If the values are _tabulation_ separated

The separated values file must be in the ``user-files/data`` folder.

The first line of the file must contain the label of the values:

::

    username,password
    john,smith21
    john,doe43

These labels will be used as keys to the values stored in the session,
for example ``${username}``.

Using Escaping Character *(since 1.1.0)*
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you choose to create a CSV file but some values contain the ``,``
character, you can escape them with the ``\`` character.

If you want to use another escaping character, you can set one while
declaring your feeder:

.. code:: scala

    val userCredentials = csv("user_credentials.csv", '#')

JDBC *(since 1.1.0)*
~~~~~~~~~~~~~~~~~~~~

Gatling provide support for JDBC data. You can get values from a
database instead of a file. The method signatures are all the same for
every supported database:

.. code:: scala

    jdbcFeeder( databaseURL: String, username: String, password: String, sql: String)

The ``databaseURL`` must be a JDBC URL (ie:
``jdbc:postgresql:gatling``), the ``username`` and ``password`` are the
credentials to access the database and ``sql`` is the request that will
get the values needed.

Only JDBC4 drivers are supported, so that they automatically registers
to the DriverManager.

    Note: Do not forget to add the required JDBC driver jar in the
    classpath (``lib/`` folder in the bundle)

Redis *(since 1.2.2)*
~~~~~~~~~~~~~~~~~~~~~

Redis can be used as a Data Source. The dynamic data can be loaded on a
Redis List, popped out for each http request and the value placed in a
session variable. Since v2.1.14 Redis supports mass insertion of data
from a [file] (http://redis.io/topics/mass-insert). It is possible to
load millions of keys in a few seconds in Redis and Gatling will read
them off memory directly.

For i.e : A simple Scala function to generate a file with 1 million
different urls ready to be loaded in a Redis list named URLS :

.. code:: scala

    import com.excilys.ebi.gatling.core.feeder.redis.util._

    def generateOneMillionUrls() = {
      val fileLocation = "/tmp/loadtest.txt"
      val writer = new PrintWriter(new File(fileLocation))
      try {
        for (i <- 0 to 1000000) {
          val url = "test?id=" + i.toString()
          writer.write(generateRedisProtocol("LPUSH", "URLS", url))
        }
      } finally {
        writer.close()
      }
    }

The urls can then be loaded in Redis using the following command :

``cat /tmp/loadtest.txt | redis-cli --pipe``

An example simulation to use this redis feeder can be viewed
`here <https://gist.github.com/2888230>`__.

    At the moment only queue feeder strategy is supported for Redis.

Built-ins: getting a Feeder from a Data Source
----------------------------------------------

Gatling provides some syntactic sugars for converting Data Source into
Feeders.

Queue strategy *(since 1.0)*
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The default strategy (ie: if you don't specify one) is the queue
strategy. Each time ``feed`` is called, the first record of the feeder
is removed from the queue and injected into the session.

Be careful while using this strategy, the feeder source must contain
enough records for the simulation; if not, the simulation will stop when
the queue is empty.

Example:
``scala csv("user_credentials.csv").queue csv("user_credentials.csv")        // It is the same as above since queue is the default strategy``

Random strategy *(since 1.0)*
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For this strategy, each time ``feed`` is called, a random record is
picked inside the feeder and injected into the session.

The records are not removed from the feeder when injected.

.. code:: scala

    csv("user_credentials.csv").random

Circular strategy *(since 1.1)*
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The circular strategy will loop on the values contained in the feeder.
If there are N values in the feeder and the ``feed`` method has been
called N+1 times, then, the record1 will be injected.

.. code:: scala

    csv("user_credentials.csv").circular

Concurrent Queue strategy *(since 1.3)*
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The above built-ins are not thread-safe, mainly because you usually
don't need sync as long as:

-  you only pop from a given feeder at one and only one place in your
   scenario
-  or, you don't really care about sync in your use case (ex: do you
   really really need sync on a random feeder?)

This works well thanks to `how Akka works with the Java Memory
Model <http://doc.akka.io/docs/akka/snapshot/general/jmm.html>`__.

For those who don't fall in one of the above cases, please use this
strategy.

.. code:: scala

    csv("user_credentials.csv").concurrentQueue

Custom Feeders
--------------

Feeder is a simple abstract class with one signe method called ``next``
that return a Map[String, T] of key/values.

The example below reuses the Gatling built-ins from a static Data
Source.

.. code:: scala

    val feeder = Array(Map("foo" -> "bar", "baz" -> "qux").circular

The example below builds a feeder that generated random user data.

.. code:: scala

    val myCustomFeeder = new Feeder[String] {
      import org.joda.time.DateTime
      import scala.util.Random

      private val RNG = new Random

      // random number in between [a...b]
      private def randInt(a:Int, b:Int) = RNG.nextInt(b-a) + a

      private def daysOfMonth(year:Int, month:Int) = new DateTime(year, month, 1, 0, 0, 0, 000).dayOfMonth.getMaximumValue

      // always return true as this feeder can be polled infinitively
      override def hasNext = true

      override def next: Map[String, String] = {
        val email = scala.math.abs(java.util.UUID.randomUUID.getMostSignificantBits) + "_gatling@dontsend.com"
        val year = randInt(1945, 1994)
        val month = randInt(1, 12)
        val day = randInt(1, daysOfMonth(year, month))

        Map("contactEmail" -> email, 
            "birthdayYear" -> year.toString, 
            "birthdayMonth" -> month.toString, 
            "birthdayDay" -> day.toString)
        }
    }

