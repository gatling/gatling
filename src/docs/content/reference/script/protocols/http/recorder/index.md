---
title: Recorder
seotitle: Gatling HTTP protocol reference - Recorder
description: How to use the Recorder and its proxy and HAR modes to capture HTTP traffic from your browser and turn it into a Gatling load test.
lead: Learn how to configure your Recorder and run it, either as an HTTP proxy or a HAR converter
date: 2021-04-20T18:30:56+02:00
lastmod: 2022-12-14T21:30:56+02:00
---

The Gatling Recorder helps you to quickly generate scenarios, by either acting as a HTTP proxy between the browser and the HTTP server or converting HAR (Http ARchive) files.
Either way, the Recorder generates a simple simulation that mimics your recorded navigation.

If you're using the bundle or the Maven plugin, you can launch the Recorder with the following command:

{{< code-toggle console >}}
Linux/MacOS: ./mvnw gatling:recorder
Windows: mvnw.cmd gatling:recorder
{{</ code-toggle >}}

For the JavaScript SDK:

```console
npx gatling recorder
```
{{< alert tip >}}
The Recorder is also available for the [sbt]({{< ref="../integrations/build-tools/sbt-plugin/" >}}) and [Gradle]({{< ref="../integrations/build-tools/gradle-plugin/" >}}) build tool plugins. See their respective documentation for more information.
{{< /alert >}}


You will get a window that looks like this one:

{{< img src="recorder.png" alt="recorder.png" >}}

## Configuration

### Listening proxy port

In the Recorder, you have to define one port (for both HTTP and HTTPS): the local proxy port. This is the port your browser must connect to so that the Recorder is able to capture your navigation.

{{< img src="recorder-listening-port.png" alt="recorder-listening-port.png" >}}

Then, you have to configure your browser to use the defined port.

Here is how to do with Firefox, open the browser Advanced settings, then go to the Network panel and update the connection settings:

{{< img src="recorder-browser_advanced_settings.png" alt="recorder-browser_advanced_settings.png" >}}

### HTTPS mode

On the contrary to regular proxies that act as a pass-though, the recorder acts as a man-in-the-middle and decrypt your HTTPS traffic.
The consequence is that it's identified by browsers as a security threat, so, one way or the other, you have to tell your browser that everything is fine.

The Recorder has 3 modes:

#### Certificate Authority {#https-ca}

In this mode, the Recorder will use a private [Certificate Authority (CA)](https://en.wikipedia.org/wiki/Certificate_authority) to generate on-the-fly properly signed certificates for every visited domain.

You can either ask the Recorder to generate the CA certificate and private key files for you, or provide your own ones.

{{< alert tip >}}
You'll have to import the CA certificate into your browser's keystore:
* official documentation for [desktop Google Chrome](https://support.google.com/chrome/a/answer/6342302?hl=en)
* official documentation for [desktop Firefox](https://support.mozilla.org/en-US/kb/setting-certificate-authorities-firefox)
* on OSX for iPhone, IPad, you simply have to send you the certificate file by email, and then open the attached file
* official documentation for [Android](https://support.google.com/nexus/answer/2844832?hl=en).
{{< /alert >}}

#### Provided KeyStore {#https-keystore}

You can pass a full Java keyStore (JKS or PKCS#12 format) that contains the certificate to be used.
This mode is useful if you have already generated a Java keystore for your application and want to reuse it for recording.

{{< alert tip >}}
You'll have to import the CA certificate into your browser's keystore, as described above.
{{< /alert >}}

#### Self-signed Certificate (legacy) {#https-self-signed}

The recorder will use the same self-signed certificate for every domain.
This mode is the legacy one and is becoming more and more complicated to use due to the security hardening trend in web browsers.

Browsers will prompt a security alert, and ask you if you want to add a security exception for this domain.
If it doesn't do so, it means that you've already registered a validated (by a standard Certificate Authority) certificate and it refuses to replace it by a less secured one.
You then have to remove it from your certificate registry.

### Outgoing proxy

If you must access your web application through a proxy, you can set it up in this section. Two different ports can be defined for the outgoing proxy (HTTP & HTTPS).

{{< alert tip >}}
Even if HTTP and HTTPS are on the same port for the outgoing proxy, you need to explicitly specify both.
{{< /alert >}}

### Filters

Allow you to filter out some requests you don't want to record. For example, if you don't want to record any CSS files, you can add in the blacklist section the following Java regex `.*\\.css`.

The order of evaluation between the whitelist and the blacklist entries can be determined with the *strategy* setting. You can either evaluate the whitelist or the blacklist first.

### Embedded resources fetching

If you check the 'Infer html resources?' option, the Recorder will fetch the embedded HTML resources as follows:

* Add `inferHtmlResources` with the proper white/black lists on the HTTP protocol definition.
* Parse HTML response body to retrieve embedded HTML resources.
* Filter requests corresponding to embedded HTML resources from resulting `Scenario`.

{{< alert warning >}}
Gatling can't retrieve all the embedded resources in HTML as images embedded in a css embedded in HTML.

This remaining resources are currently loaded sequentially as regular requests.
{{< /alert >}}

### HTTP request naming
By default, Gatling will use the prefix `request_` for recorded and converted http requests.
When the 'Use class name as request prefix?' option is enabled, http requests will use the
simulation class as prefix for the request name instead.

### Response bodies

When the 'Save & check response bodies?' option is enabled, response bodies will be dumped in the same folder as the request bodies, and the simulation will contain extra checks using [RawFileBody]({{< ref "request#rawfilebody" >}}) to ensure the actual bodies are matching the dumped ones. You might want to edit these checks, for example to parametrize the expected bodies, using [ElFileBody]({{< ref "request#elfilebody" >}}) instead.

## Running

Once everything has been configured, press the **Start** button to launch the recorder.

### Recorded Events

As you navigate through your application, the recorder will log three kinds of events:

* **Requests**: The requests sent by the browser.
* **Pauses**: The time between each request.
* **Tags**: Manually set markers.

### Tag Events

To make your scenario more understandable, you can add tags, they will appear as comments in the scenario: `/* my tag */`

To add a tag, fill in the text field provided and press the **Add** button. After that, the tag will be displayed in the list below.

For example: `TAG | my tag`

### Stop

When you have finished recording your scenario, press the **Stop** button to save it in the format defined in the configuration.

## HAR Files

You can import a HAR file (Http Archive) into the Recorder and convert it to a Gatling simulation.

HAR files can be obtained using the Chrome Developer Tools or with Firebug and the NetExport Firebug extension.

With Chrome Developer Tools, go to *Network* tab, and make sure you've selected the *Preserve log* checkbox, otherwise the log is reset when you change page.
Select the requests you want to export, then right click and select *Copy All as HAR* and save what's in your clipboard into a file.

Please don't use [Charles Proxy](http://www.charlesproxy.com) for this.
Charles is an amazing tool and has an HAR export feature, but it's a proxy, so when you use it, you change the HTTP behavior, and the HAR would contain requests that should be here, such as CONNECT requests.

To import a HAR file, select the *HAR converter* mode in the top right dropdown in the Recorder.

## Headless mode

Along the GUI mode, Gatling also offers a simple CLI interface, facilitating the automation of recording or converting simulations from HAR files.
The Headless mode can be enabled either from the `recorder.conf` file or with the `-cli`/`--headless` command line option.
Both 'Proxy' and 'HAR' modes are supported (you can set which mode to use using the `-m`/`--mode` command line option).

### Proxy

In 'Proxy mode', the Recorder will start listening for requests from your browser right away.
To stop the Recorder and create the Simulation, you have to 'kill' the Recorder by either:

* Sending a 'kill' signal with `CTRL-C`
* Killing the Recorder's process, using the Recorder process ID written to the `.gatling-recorder-pid` file: `cat .gatling-recorder-pid | xargs kill`

### HAR Converter

In 'Har' mode, the Recorder will convert the provided HAR file to a Simulation and exits.

## Command-line options

For those who prefer the command line, command line options can be passed to the Recorder:

| Option (short)                      | Option (long)                                         | Description                              |
|-------------------------------------|-------------------------------------------------------|------------------------------------------|
| `-lp <port>`                        | `--local-port <port>`                                 | Local Proxy HTTP/HTTPS port              |
| `-ph <port>`                        | `--proxy-host <port>`                                 | Outgoing proxy host                      |
| `-pp <port>`                        | `--proxy-port <port>`                                 | Outgoing proxy port                      |
| `-pps <port>`                       | `--proxy-port-ssl <port>`                             | Outgoing proxy SSL port                  |
| `-sf <path>`                        | `--simulations-folder <path>`                         | Output folder for generated simulations  |
| `-rf <path>`                        | `--resources-folder <path>`                           | Output folder for generated resources    |
| `-cn <className>`                   | `--class-name <className>`                            | Name of the generated simulation         |
| `-pkg <pkgName>`                    | `--package <pkgName>`                                 | Package of the generated simulation      |
| `-enc <encoding>`                   | `--encoding <encoding>`                               | Encoding used in the Recorder            |
| <code>-fr <true&#124;false></code>  | <code>--follow-redirect <true&#124;false></code>      | Enable *Follow Redirects*                |
| <code>-ar <true&#124;false></code>  | <code>--automatic-referer <true&#124;false></code>    | Enable *Automatic Referers*              |
| <code>-fhr <true&#124;false></code> | <code>--fetch-html-resources <true&#124;false></code> | Enable *Fetch html resources*            |
| <code>-m <Proxy&#124;Har></code>    | <code>--mode <Proxy&#124;Har></code>                  | Recorder mode to use                     |
| <code>-cli <true&#124;false></code> | <code>--headless <true&#124;false></code>             | Run Recorder in headless mode            |
| `-hf <path>`                        | `--har-file <path>`                                   | The HAR file to convert (if mode is Har) |

{{< alert tip >}}
Command-line options override saved preferences.
{{< /alert >}}

## System properties

There are 2 system properties to control the use of a custom certificate keystore for the proxy:

* `gatling.recorder.keystore.path`
* `gatling.recorder.keystore.passphrase`
