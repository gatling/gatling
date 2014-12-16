.. _recorder:

########
Recorder
########

The Gatling Recorder helps you to quickly generate scenarios, by either acting as a HTTP proxy between the browser and the HTTP server or converting HAR (Http ARchive) files.
Either way, the Recorder generates a simple simulation that mimics your recorded navigation.

If you're using the bundle, you can launch it with the following script ``$GATLING_HOME/bin/recorder.sh``.
You will get a window that looks like this one:

.. image:: img/recorder.png

Configuration
=============

Listening proxy port
--------------------

In the Recorder, you have to define one port (for both HTTP and HTTPS): the local proxy port. This is the port your browser must connect to so that the Recorder is able to capture your navigation.

.. image:: img/recorder-listening-port.png

Then, you have to configure your browser to use the defined port.

Here is how to do with Firefox, open the browser settings:

.. image:: img/recorder-browser_settings.png

Then, update the connection settings:

.. image:: img/recorder-browser_advanced_settings.png


HTTPS mode
----------

On the contrary to regular proxies that act as a pass-though, the recorder acts as a man-in-the-middle and decrypt your HTTPS traffic.
The consequence is that it's identified by browsers as a security threat, so, one way or the other, you have to tell your browser that everything is fine.

The Recorder has 3 modes:

* Self-signed certificate (historical default)

The recorder will use the same self-signed certificate for every domain.
Browsers will prompt a security alert, and ask you if you want to add a security exception for this domain.
If it doesn't do so, it means that you've already registered a validated (by a standard Certificate Authority) certificate and it refuses to replace it by a less secured one.
You then have to remove it from your certificate registry.

.. warning:: Browsers will only prompt a security alert for the page domain, not for resource ones (typically, CNDs).
             The Recorder will list all such domains, you'll then have to directly visit every one the them and add a security exception for each.

* Provided KeyStore

One can pass a full Java keyStore (JKS or PKCS#12 format) that contains the certificate to be used.

* On-the-fly certificate generation

This mode takes a Certificate Authority (certificate and private key, in PEM format) and generates signed certificates for every visited domain.
It requires you to import the CA certificate into your browser's CA list.

You can either ask Gatling to generate those files for you, or provide a CA of your own.

Outgoing proxy
--------------

If you must access your web application through a proxy, you can set it up in this section. Two different ports can be defined for the outgoing proxy (HTTP & HTTPS).

.. note:: Even if HTTP and HTTPS are on the same port for the outgoing proxy, you need to explicitly specify both.

Filters
-------

Allow you to filter out some requests you don't want to record. For example, if you don't want to record any CSS files, you can add in the blacklist section the following Java regex ``.*\\.css``.

The order of evaluation between the whitelist and the blacklist entries can be determined with the *strategy* setting. You can either evaluate the whitelist or the blacklist first.

Embedded resources fetching
---------------------------

If you check the 'Infer html resources?' option, the Recorder will fetch the embedded HTML resources as follow:

* Add ``inferHtmlResources`` with the proper white/black lists on the HTTP protocol definition.
* Parse HTML response body to retrieve embedded HTML resources.
* Filter requests corresponding to embedded HTML resources from resulting ``Scenario``.

.. warning:: Gatling can't retrieve all the embedded resources in HTML as images embedded in a css embedded in HTML.
             This remaining resources are currently loaded sequentially as regular requests.

Response bodies
---------------

When the 'Save & check response bodies?' option is enabled, response bodies will be dumped in the same folder as the request bodies, and the simulation will contain extra checks using :ref:`RawFileBody <http-request-body-rawfile>` to ensure the actual bodies are matching the dumped ones. You might want to edit these checks, for example to parametrize the expected bodies, using :ref:`ELFileBody <http-request-body-elfile>` instead.

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

Please don't use `Charles Proxy <http://www.charlesproxy.com>`__ for this.
Charles is an amazing tool and has an HAR export feature, but it's a proxy, so when you use it, you change the HTTP behavior, and the HAR would contain requests that should be here, such as CONNECTs.

To import a HAR file, select the *HAR converter* mode in the top right dropdown in the Recorder.

Certificates
============

Recording browsers' http traffic over ssl is possible usually as the browser allows the user the option to accept a self signed certificate.
Devices other than browsers may not provide that option, making it impossible to record SSL traffic on those devices.

A set of certificates can be generated to allow devices to trust the recorder proxy.

As creating fake certificates in this way could be misused the lifetime of the certificates can be set to 1 day to ensure that they cannot be misused at a later date.

Steps:

* Generate a Certificate Authority certificate
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

Install the CA certificate into the client device - gatlingCA.crt

Configure the recorder to use the custom java keystore - gatling-custom.jks



Command-line options
====================

For those who prefer the command line, command line options can be passed to the Recorder:

+--------------------+-------------------------------------+-----------------------------------------+
| Option (short)     | Option (long)                       | Description                             |
+====================+=====================================+=========================================+
| -lp <port>         | --local-port <port>                 | Local Proxy HTTP/HTTPS port             |
+--------------------+-------------------------------------+-----------------------------------------+
| -ph <port>         | --proxy-host <port>                 | Outgoing proxy host                     |
+--------------------+-------------------------------------+-----------------------------------------+
| -pp <port>         | --proxy-port <port>                 | Outgoing proxy port                     |
+--------------------+-------------------------------------+-----------------------------------------+
| -pps <port>        | --proxy-port-ssl <port>             | Outgoing proxy SSL port                 |
+--------------------+-------------------------------------+-----------------------------------------+
| -of <path>         | --output-folder <path>              | Output folder for generated simulations |
+--------------------+-------------------------------------+-----------------------------------------+
| -bdf <path>        | --bodies-folder <path>              | Folder for bodies                       |
+--------------------+-------------------------------------+-----------------------------------------+
| -cn <className>    | --class-name <className>            | Name of the generated simulation        |
+--------------------+-------------------------------------+-----------------------------------------+
| -pkg <packageName> | --package <packageName>             | Package of the generated simulation     |
+--------------------+-------------------------------------+-----------------------------------------+
| -enc <encoding>    | --encoding <encoding>               | Encoding used in the Recorder           |
+--------------------+-------------------------------------+-----------------------------------------+
| -fr <true|false>   | --follow-redirect <true|false>      | Enable *Follow Redirects*               |
+--------------------+-------------------------------------+-----------------------------------------+
| -ar <true|false>   | --automatic-referer <true|false>    | Enable *Automatic Referers*             |
+--------------------+-------------------------------------+-----------------------------------------+
| -fhr <true|false>  | --fetch-html-resources <true|false> | Enable *Fetch html resources*           |
+--------------------+-------------------------------------+-----------------------------------------+

.. note:: Command-line options override saved preferences.

System properties
=================

There are 2 system properties to control the use of a custom certificate keystore for the proxy:

* ``gatling.recorder.keystore.path``
* ``gatling.recorder.keystore.passphrase``

