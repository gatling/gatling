---
title: "Migrating from 3.4 to 3.5"
description: ""
lead: ""
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

## Scala 2.13

Gatling 3.5 is now compiled against Scala 2.13, which is not binary compatible with Scala 2.12.
Any code compiled with a previous version must be recompiled in order to be executed with Gatling 3.5.

## Postfix Operators Notation

Scala 2.13 is paving the way for removing postfix operators, eg stop writing `5 milliseconds` in favor of `5.milliseconds`.

You can still compile code with this syntax if you enable `-language:postfixOps` in the Scala compiler options.

Gatling does enable this option in the zip bundle and in the maven, gradle and sbt plugins.

If you're running from IntelliJ, you have to enable it in your Preferences:

{{< img src="intellij-scalac-postfix.png" alt="intellij-scalac-postfix.png" >}}

Still, we recommend that your progressively adopt the correct coding style as Scala will drop postfix operators in a future release.
