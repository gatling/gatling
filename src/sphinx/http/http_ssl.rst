:tocdepth: 2

.. _http-ssl:

###
SSL
###

.. _http-ssl-sslcontext:
SSLContext
==========

By default, each virtual user will have its own ``SSLContext`` and ``SSLSession``.
This behavior is realistic when it comes to simulating web traffic so your server has to deal with the proper number of ``SSLSession``.

You can only have a shared ``SSLContext`` if you decide to :ref:`shareConnections <http-protocol-connection-sharing>`.

.. _http-ssl-openssl:
Disabling OpenSSL
=================

By default, Gatling uses `BoringSSL <https://opensource.google.com/projects/boringssl>`_ to perform TLS.
This implementation is more efficient than the JDK's one, especially on JDK8.
It's also the only supported solution for HTTP/2 in Gatling with JDK8.

If you want to revert to using JDK's implementation, you can set the ``gatling.http.ahc.useOpenSsl`` property to ``false`` in ``gatling.conf``

.. _http-ssl-sni:

Disabling SNI
=============

By default, since JDK7, JDK enables `SNI <http://en.wikipedia.org/wiki/Server_Name_Indication>`_ by default.
This can cause SSL handshake exceptions, such as ``handshake alert:  unrecognized_name`` when server names are not properly configured on the server side.
Browsers are more loose than JDK regarding this.

If you want to disable SNI, you can set the ``gatling.http.ahc.enableSni`` property to ``false`` in ``gatling.conf``.

.. _http-ssl-stores:

Configuring KeyStore and TrustStore
===================================

Default Gatling TrustStore is very permissive and doesn't validate certificates,
meaning that it works out of the box with self-signed certificates.

You can pass your own keystore and trustore in ``gatling.conf``.

:ref:`perUserKeyManagerFactory <http-protocol-kmf>` can be used to set the ``KeyManagerFactory`` for each virtual user.
