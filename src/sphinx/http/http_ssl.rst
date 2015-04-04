:tocdepth: 2

.. _http-ssl:

###
SSL
###

.. _http-ssl-sni:

Disabling SNI
=============

By default, since JDK7, JDK enables `SNI <http://en.wikipedia.org/wiki/Server_Name_Indication>`_ by default.
This can cause SSL handshake exceptions, such as ``handshake alert:  unrecognized_name`` when server names are not properly configured on the server side.
Browsers are more loose than JDK regarding this.

If you want to disable SNI, you can set the following System property: ``-Djsse.enableSNIExtension=false``.

.. _http-ssl-stores:

Configuring KeyStore and TrustStore
===================================

Default Gatling TrustStore is very permissive and doesn't validate certificates,
meaning that it works out of the box with self-signed certificates.

.. _http-ssl-stores-shared:

Shared Stores
-------------

One can provide custom KeyStores and TrustStores.
Configuration can be passed with the standard config mechanism, either in ``gatling.conf`` or with System properties::

  "gatling.http.ssl.trustStore.type"      (optional)
  "gatling.http.ssl.trustStore.file"      (required, can be an absolute path, or a classpath location)
  "gatling.http.ssl.trustStore.password"  (optional)
  "gatling.http.ssl.trustStore.algorithm" (optional)

  "gatling.http.ssl.keyStore.type"        (optional)
  "gatling.http.ssl.keyStore.file"        (required, can be an absolute path, or a classpath location)
  "gatling.http.ssl.keyStore.password"    (optional)
  "gatling.http.ssl.keyStore.algorithm"   (optional)

.. _http-ssl-stores-per-user:

Per Virtual User Stores
-----------------------

Sometimes, one might want to have virtual users with different KeyStores and TrustStores.

As KeyStores and TrustStores are define at the HTTP engine instance level, one first has to use :ref:`disableClientSharing <http-protocol-client-sharing>`.

Then, simply pass the same properties as session attributes (manually or with a feeder).
