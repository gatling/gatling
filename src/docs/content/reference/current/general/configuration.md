---
title: "Configuration"
description: "Configuration of Gatling"
lead: "Configure the logs with logback.xml, the configuration with gatling.conf, and the zip bundle command options"
date: 2021-04-20T18:30:56+02:00
lastmod: 2021-04-20T18:30:56+02:00
---

Gatling can be configured and optimized in three ways:

* with configuration files, located in `conf` directory
* with command line options
* with `$JAVA_OPTS` environment variable

## Configuration files

### logback.xml

This file allows you to configure the log level of Gatling.
For further information, you should have a look at [Logback Documentation](http://logback.qos.ch/manual/index.html).

{{< alert tip >}}
In order to log requests and responses, uncomment the dedicated loggers in the [default logging configuration file](https://github.com/gatling/gatling/blob/master/gatling-core/src/main/resources/logback.dummy).
{{< /alert >}}

### gatling.conf {#gatling-conf}

Gatling configuration is based on the great [Typesafe Config library](https://github.com/typesafehub/config).

Gatling configuration files, such as the default configuration file uses the [HOCON format](https://github.com/typesafehub/config/blob/master/HOCON.md).

Gatling uses a fallback strategy, where:

**System properties > gatling.conf > gatling-defaults.conf**

In the bundle packaging, `gatling.conf` is placed in the `conf` directory. It serves as an easy-to-edit base: all properties are commented and all values are the defaults.

In maven/sbt/gradle projects, it must be placed in the `resources`.

The name of this file can be overriding from a System property named `gatling.conf.file`, eg `-Dgatling.conf.file=gatling-special.conf`.

`gatling-defaults.conf` is shipped in the gatling-core jar and should not be edited.

If you want to override default values, you have two possibilities:

* change the value in `gatling.conf`.
* set a System property (the name of the property must match [HOCON Path](https://github.com/typesafehub/config/blob/master/HOCON.md#paths-as-keys))

{{< alert warning >}}
When editing `gatling.conf`, don't forget to remove the leading `#` that comments the line, otherwise your change will be ineffective.
{{< /alert >}}

## Zip Bundle Command Line Options {#cli-options}

Gatling can be started with several options listed below:

| Option (short)     | Option (long)                      | Description                                                                                        |
| --- | --- | --- |
| `-h`               | `--help`                           | Help                                                                                               |
| `-nr`              | `--no-reports`                     | Runs simulation but does not generate reports                                                      |
| `-ro <folderName>` | `--reports-only <folderName>`      | Generates the reports for the simulation log file located in](gatling_home>/results/`<folderName>` |
| `-rf <path>`       | `--results-folder](path>`          | Uses `<path>` as the folder where results are stored                                               |
| `-rsf <path>`      | `--resources-folder](path>`        | Uses `<path>` as the folder where resources are stored                                             |
| `-sf <path>`       | `--simulations-folder](path>`      | Uses `<path>` as the folder where simulations are stored                                           |
| `-bf <path>`       | `--binaries-folder](path>`         | Uses `<path>` as the folder where simulation binaries are stored                                   |
| `-s <className>`   | `--simulation](className>`         | Uses `<className>` as the name of the simulation to be run                                         |
| `-rd <description>`| `--run-description](description>`  | A short `<description>` of the run to include in the report                                        |

## $JAVA_OPTS

Default command line options for JAVA are set in the launch scripts.
You can use the JAVA_OPTS var to override those defaults, eg:

```shell
$ JAVA_OPTS="myAdditionalOption" bin/gatling.sh
```

[Default configuration file](https://github.com/gatling/gatling/blob/master/gatling-core/src/main/resources/gatling-defaults.conf)
