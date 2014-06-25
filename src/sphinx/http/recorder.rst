.. _recorder:

########
Recorder
########

The Gatling Recorder helps you to quickly generate scenarios. It acts as a HTTP proxy between the browser and the HTTP server. While you navigate through the application, it records all HTTP exchange and when done, generates the scenario simulating what you just did.

Launch it from the bundle with the following script ``$GATLING_HOME/bin/recorder.sh``.
You will get a window that looks like this one:

.. image:: img/recorder.png

Configuration
=============

Local proxy ports
-----------------

In the Recorder, you have to define two ports: one for listening to HTTP traffic and one for HTTPS traffic. Then, you have to configure your browser to use the definied ports.

Here is how to do with Firefox, open the browser settings:

.. image:: img/recorder-browser_settings.png

Then, update the connection settings:

.. image:: img/recorder-browser_advanced_settings.png

.. note:: HTTPS port isn't mandatory if you don't plan to use an HTTPS connection.


Outgoing proxy
--------------

If you must access your web application through a proxy, you can set it up in this section. As the configuration of local ports, two different ports can be defined for the outgoing proxy (HTTP & HTTPS).

.. note:: If HTTP and HTTPS are on the same port for the outgoing proxy, you need to explicitly specify both.


Filters
-------

Allow you to filter out some requests you don't want to record. For example, if you don't want to record any CSS files, you can add in the blacklist section the following Java regex ``.*\\.css``.

The order of evaluation between the whitelist and the blacklist entries can be determined with the *strategy* setting. You can either evaluate the whitelist or the blacklist first.

Embedded resources fetching
---------------------------

If you check the option 'Fetch html resources?' option, the Recorder will fetch the embedded HTML resources as follow:

* Add ``fetchHtmlResources`` with the proper white/black lists on the HTTP protocol definition.
* Parse HTML response body to retrieve embedded HTML resources.
* Filter requests corresponding to embedded HTML resources from resulting ``Scenario``.

.. warning:: Gatling can't retrieve all the embedded resources in HTML as images embedded in a css embedded in HTML.
             This remaining resources are currently loaded sequentially as regular requests.

Running
=======

Once everything has been configured, press the **Start** button to launch the recorder.

Recorded Events
---------------

As you navigate through your application, the recorder will log three kinds of events:

* **Requests**: The requests sent by the browser.
* **Pauses**: The time between each request.
* **Tags**: Manually set markers.

Tag Events
----------

To make your scenario more understandable, you can add tags, they will appear as comments in the scenario: ``/* my tag */``

To add a tag, fill in the text field provided and press the **Add** button. After that, the tag will be displayed in the list below.

For example: ``TAG | my tag``


Stop
----

When you have finished recording your scenario, press the **Stop** button to save it in the format defined in the configuration.

HAR Files
=========

You can import a HAR file (Http Archive) into the Recorder and convert it to a Gatling simulation.

HAR files can be obtained using the Chrome Developer Tools or with Firebug and the NetExport Firebug extension.

With Chrome Developer Tools, go to *Network* tab, and make sure you've selected the *Preserve log* checkbox, otherwise the log is reset when you change page.
Select the requests you want to export, then right click and select *Copy All as HAR* and save what's in your clipboard into a file.

Please don't use `Charles Proxy <http://www.charlesproxy.com>`_ for this.
Charles is an amazing tool and has an HAR export feature, but it's a proxy, so when you use it, you change the HTTP behavior, and the HAR would contain requests that should be here, such as CONNECTs.

To import a HAR file, select the *HAR converter* mode in the top right dropdown in the Recorder.

Certificates
============

Recording browsers' http traffic over ssl is possible ususally as the browser allows the user the option to accept a self signed certificate.
Devices other than browsers may not provide that option, making it impossible to record SSL traffic on those devices.

A set of certificates can be generated to allow devices to trust the recorder proxy.

As creating fake certificates in this way could be misused the lifetime of the certificates can be set to 1 day to ensure that they cannot be misused at a later date.

Steps:

* Generate a Certificate Authority certficate
* Generate a server certificate signed by the CA certificate for the proxy. The certificate is generated against the site domain name(s) being recorded
* Import the server certificate and Chain (CA cert) into a java keystore
* Import the CA certificate into the Device/Client
* configure the recorder to use the custom keystore

Generating the custom certificates
----------------------------------

- Certificate Authority

OpenSSL commands::

  openssl genrsa -out rootCA.key 2048
  openssl req -x509 -new -nodes -key rootCA.key -days 1 -out rootCA.pem
  openssl x509 -outform der -in rootCA.pem -out gatlingCA.crt

- Proxy SSL certificate

.. note:: the 'common name' (which is deprecated but still works) and/or 'Subject Alternative Name' should match the domain name(s) that you are testing through the proxy.

OpenSSL commands::

  openssl genrsa -out device.key 2048
  openssl req -new -key device.key -out device.csr
  openssl x509 -req -in device.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out device.crt -days 1
  openssl pkcs12 -export -in device.crt -inkey device.key -out server.p12 -name gatling -CAfile rootCA.pem -caname gatling -chain
  keytool -importkeystore -deststorepass gatling -destkeypass gatling -destkeystore gatling-custom.jks  -srckeystore server.p12 -srcstoretype PKCS12 -srcstorepass gatling -alias gatling

Configuring / Set up
--------------------

Install the CA certficate into the client device - gatlingCA.crt

Configure the recorder to use the custom java keystore - gatling-custom.jks



Command-line options
====================

For those who prefer the command line, command line options can be passed to the gatling-recorder:

* **-lp**: Local port (alias = **--local-port**)
* **-lps**: Local SSL port (alias = **--local-port-ssl**)
* **-ph**: Outgoing proxy host (alias = **--proxy-host**)
* **-pp**: Outgoing proxy port (alias = **--proxy-port**)
* **-pps**: Outgoing proxy SSL port (alias = **--proxy-port-ssl**)
* **-of**: Output folder for results (alias = **--output-folder**)
* **-rbf**: Folder for requests bodies (alias = **--request-bodies-folder**)
* **-cn**: Name of the generated class (alias = **--class-name**)
* **-pkg**: Package of the generated class (alias = **--package**)
* **-enc**: Encoding used in the Recorder (alias = **--encoding**)
* **-fr**: Enable *Follow Redirects* (alias = **--follow-redirect**)
* **-ar**: Enable *Automatic Referers* (alias = **--automatic-referer**)
* **-fhr**: Enable *Fetch html resources* (alias = **--fetch-html-resources**)

.. note:: Command-line options override saved preferences.

System properties
=================

There are 2 system properties to control the use of a custom certificate keystore for the proxy:

* gatling.recorder.keystore.path
* gatling.recorder.keystore.passphrase

