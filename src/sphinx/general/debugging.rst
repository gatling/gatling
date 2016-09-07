.. _debugging:

#########
Debugging
#########

Print Session Values
====================

Print all the values within a session with this function literal.

.. includecode:: code/ComputerWorld.scala#print-all-session-values

Print a session value

.. includecode:: code/ComputerWorld.scala#print-session-value

Logback
=======

There's a logback.xml file in the Gatling conf directory.
You can either set the log-level to TRACE to log all HTTP
requests and responses or DEBUG to log failed HTTP request
and responses.

It will by default print debugging information to the console,
but you can add a file appender,

.. code-block:: xml

    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>../gatling.log</file>
        <append>true</append>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%-5level] %logger{15} - %msg%n%rEx</pattern>
        </encoder>
    </appender>


And reference that appender

.. code-block:: xml

    <root level="WARN">
        <appender-ref ref="FILE" />
    </root>


This can be useful if you run at one user and remove all
logging apart from the HTML, and open the file in your browser.
