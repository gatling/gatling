---
title: "Configuration"
description: "Configuration of Gatling"
lead: "Configure the logs with logback.xml, the configuration with gatling.conf, and the zip bundle command options"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
weight: 2030800
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

`gatling.conf` is the default name of the user defined file. It's resolved from the ClassLoader, not the filesystem, meaning it must be placed in `src/test/resources` for a maven/gradle/sbt project and in `conf` in the bundle distribution. 

This file name can be changed with a System property named `gatling.conf.file`, eg `-Dgatling.conf.file=gatling-special.conf`. Again, beware it's loaded from the ClassLoader, not the filesystem, eg:

* `-Dgatling.conf.file=src/test/resource/gatling-special.conf` is incorrect
* `-Dgatling.conf.file=gatling-special.conf` where `gatling-special.conf` is placed in `src/test/resource` is correct

The bundle distribution and the maven/gradle/sbt plugins demo projects contain an easy-to-edit `gatling.conf` file with all the available properties commented with the default values.

If you want to override default values, you have two possibilities:

* change the value in `gatling.conf`.
* set a System property (the name of the property must match the [HOCON Path](https://github.com/typesafehub/config/blob/master/HOCON.md#paths-as-keys))

{{< alert warning >}}
When editing `gatling.conf`, don't forget to remove the leading `#` that comments the line, otherwise your change will be ineffective.
{{< /alert >}}

## Zip Bundle Command Line Options {#cli-options}

Gatling can be started with several options listed below:

| Option (short)     | Option (long)                      | Description                                                                                        |
| --- | --- | --- |
| `-h`               | `--help`                           | Help                                                                                               |
| `-nr`              | `--no-reports`                     | Runs simulation but does not generate reports                                                      |
| `-ro <folderName>` | `--reports-only <folderName>`      | Generates the reports for the simulation log file located in `<gatling_home>/results/<folderName>` |
| `-rf <path>`       | `--results-folder <path>`          | Uses `<path>` as the folder where results are stored                                               |
| `-rsf <path>`      | `--resources-folder <path>`        | Uses `<path>` as the folder where resources are stored                                             |
| `-sf <path>`       | `--simulations-folder <path>`      | Uses `<path>` as the folder where simulations are stored                                           |
| `-bf <path>`       | `--binaries-folder <path>`         | Uses `<path>` as the folder where simulation binaries are stored                                   |
| `-s <className>`   | `--simulation <className>`         | Uses `<className>` as the name of the simulation to be run                                         |
| `-rd <description>`| `--run-description <description>`  | A short `<description>` of the run to include in the report                                        |

## $JAVA_OPTS

Default command line options for JAVA are set in the launch scripts.
You can use the JAVA_OPTS environment variable to override those defaults, eg:

```console
JAVA_OPTS="myAdditionalOption" bin/gatling.sh
```

