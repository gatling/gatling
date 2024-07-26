---
title: Configuration
seotitle: Gatling configuration reference
description: "How to configure Gatling: configuration file options, bundle command line options, logging."
lead: Configure the logs with logback.xml, the configuration with gatling.conf, and the zip bundle command options
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
---

Gatling can be configured and optimized in three ways:

* with configuration files, located in `conf` directory
* with command line options
* with the `$JAVA_OPTS` environment variable

## Configuration files

### logback.xml

This file allows you to configure the log level of Gatling.
For further information, you should have a look at the [Logback Documentation](http://logback.qos.ch/manual/index.html).

{{< alert tip >}}
In order to log requests and responses, uncomment the dedicated loggers in the [default logging configuration file](https://github.com/gatling/gatling/blob/main/gatling-core/src/main/resources/logback.dummy).
{{< /alert >}}

### gatling.conf {#gatling-conf}

Gatling configuration is based on the great [Typesafe Config library](https://github.com/lightbend/config).

Gatling configuration files, such as the default configuration file, use the [HOCON format](https://github.com/lightbend/config/blob/master/HOCON.md).

{{< alert tip >}}
Please check [the configuration file source on GitHub](https://github.com/gatling/gatling/blob/main/gatling-core/src/main/resources/gatling-defaults.conf) for all the available configuration options.
{{< /alert >}}

Gatling uses a fallback strategy, where:

**System properties > `gatling.conf` > gatling-defaults.conf**

`gatling-defaults.conf` is shipped in the gatling-core jar and must not be tampered.

`gatling.conf` is the default name of the user defined file. It's resolved from the ClassLoader, not the filesystem, meaning it must be placed in `src/test/resources` for a Maven, Gradle, or sbt project.

This file name can be changed with a System property named `gatling.conf.file`, eg `-Dgatling.conf.file=gatling-special.conf`. Again, beware it's loaded from the ClassLoader, not the filesystem, eg:

* `-Dgatling.conf.file=src/test/resource/gatling-special.conf` is incorrect
* `-Dgatling.conf.file=gatling-special.conf` where `gatling-special.conf` is placed in `src/test/resource` is correct

The bundle distribution and the Maven, Gradle, and sbt plugin demo projects contain an easy-to-edit `gatling.conf` file with all the available properties commented with the default values.

If you want to override default values, you have two possibilities:

* change the value in `gatling.conf`.
* set a System property (the name of the property must match the [HOCON Path](https://github.com/typesafehub/config/blob/master/HOCON.md#paths-as-keys))

{{< alert warning >}}
When editing `gatling.conf`, don't forget to remove the leading `#` that comments the line, otherwise your change will be ineffective.
{{< /alert >}}

## Command Line Options {#cli-options}

Each distribution of Gatling comes with a CLI which can be used to select run time options. Use the following commands to access the full list of available options:

| Build tool                                   | Plugin or Package manager      | Wrapper (Windows)          | Wrapper (MacOS/Linux)    |
|----------------------------------------------|-----------------------|----------------------------|--------------------------|
| Maven </br>(including the standalone bundle) | `mvn gatling:help`    | `mvnw.cmd gatling:help`    | `./mvnw gatling:help`    |
| NPM                                          | `npx gatling --help`  |             --             |            --            |
| Gradle                                       | `gradle tasks` | `gradlew.bat tasks` | `./gradlew tasks` |
| sbt                                          | `sbt help Gatling`      |             --             |            --            |

## Manage configuration values

Some Gatling functionalities require environment variables or Java system properties. This guide provides instructions for how you can apply specific values to your project. This guide does not cover setting environment variables permanently. If you wish to do this, you need to consult the documentation for your chosen shell. 

All of the following examples demonstrate the API token use case.

### Export or set an environment variable 

This procedure exports (MacOS and Linux) or sets (Windows), an environment variable for the duration of your session. Once you close or terminate the terminal session, you need to set the environment variable again. 

Setting environment variables works for each of the Gatling programming language SDKs. 

In a shell session (MacOS and Linux) or CMD session (Windows) use the following command to set an environment variable:

{{< code-toggle console >}}
Linux/MacOS: export GATLING_ENTERPRISE_API_TOKEN=<API-token-value>
Windows: set GATLING_ENTERPRISE_API_TOKEN=<API-token-value>
{{</ code-toggle >}}

You can confirm the environment variable is properly set with the following command: 

{{< code-toggle console >}}
Linux/MacOS: echo $GATLING_ENTERPRISE_API_TOKEN
Windows: echo %GATLING_ENTERPRISE_API_TOKEN%
{{</ code-toggle >}}

### Pass a Java system property

To pass a Java system property, use `-Dprop=value` with any of the JVM build tools. The following example passes and API token and starts a simulation on Gatling Enterprise:

#### Maven

{{< code-toggle console >}}
Linux/MacOS: ./mvnw -Dgatling.enterprise.apiToken=<API-token-value> gatling:enterpriseStart
Windows: mvnw.cmd -Dgatling.enterprise.apiToken=<API-token-value> gatling:enterpriseStart
{{</ code-toggle >}}

#### Gradle

{{< code-toggle console >}}
Linux/MacOS: ./gradlew -Dgatling.enterprise.apiToken=<API-token-value> gatling:enterpriseStart
Windows: gradle.bat -Dgatling.enterprise.apiToken=<API-token-value> gatling:enterpriseStart
{{</ code-toggle >}}

#### sbt

```
sbt -Dgatling.enterprise.apiToken=<API-token-value> Gatling/enterpriseStart
```

### Pass a JavaScript parameter 

The JavaScript and Typescript SDK allows you to pass command line arguments to set parameters such as an API token. To pass a parameter, use the appropriate flag and value. The following example is for an API token with the `enterprise-deploy` command:

``` console
npx gatling enterprise-deploy --api-token <API-token-value>
```

## $JAVA_OPTS

Default command line options for JAVA are set in the launch scripts.
You can use the JAVA_OPTS environment variable to override those defaults, eg:

```console
JAVA_OPTS="myAdditionalOption" bin/gatling.sh
```
