---
menutitle: Debugging 
title: Debugging guide
seotitle: Debugging guide for Gatling scripts
description: Debug Gatling scripts by printing session values or with logback.
lead: Debug Gatling scripts by printing session values or with logback.
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

## Printing Session Values

Print a session value.

{{< alert warning >}}
Only use `println` for debugging, not under load.
sysout is a slow blocking output, massively writing in here will freeze Gatling's engine and break your test.
{{< /alert >}}

{{< include-code "print-session-value" java kt scala >}}

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
