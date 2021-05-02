---
title: "Debugging"
description: "Debugging of Gatling"
lead: "Debug Gatling by printing session values or with logback"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

## Print Session Values

Print all the values within a session with this function literal.

Print a session value

{{< include-code "ComputerDatabaseSample.scala#print-session-value" scala >}}

## Logback

There's a logback.xml file in the Gatling conf directory.
You can either set the log-level to TRACE to log all HTTP requests and responses or DEBUG to log failed HTTP request and responses.

```xml
<!-- uncomment and set to DEBUG to log all failing HTTP requests -->
<!-- uncomment and set to TRACE to log all HTTP requests -->
<!--<logger name="io.gatling.http.engine.response" level="TRACE" />-->
```

It will by default print debugging information to the console, but you can add a file appender:

```xml
<appender name="FILE" class="ch.qos.logback.core.FileAppender">
  <file>PATH_TO_LOG_FILE</file>
  <append>true</append>
  <encoder>
    <pattern>%d{HH:mm:ss.SSS} [%-5level] %logger{15} - %msg%n%rEx</pattern>
  </encoder>
</appender>
```

And reference that appender:

```xml
<root level="WARN">
  <appender-ref ref="FILE" />
</root>
```

This can be useful if you run at one user and remove all logging apart from the HTML, and open the file in your browser.
