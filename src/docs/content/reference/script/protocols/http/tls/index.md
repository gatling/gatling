---
title: TLS
seotitle: Gatling HTTP protocol reference - TLS
description: How to configure TLS/HTTPS features, such as SSLContext, SNI, keystore and truststore.
lead: Configure the SSLContext, SNI, keystore and truststore
date: 2021-04-20T18:30:56+02:00
lastmod: 2024-09-12T13:29:56+02:00
---

## KeyManager and trustManager

By default, Gatling uses:
* the JVM's default KeyManager
* a fake TrustManager that trust everything. The reason is we want to work out of the box with any server certificate, even when it's issued from a custom Authority. Gatling is a load test tool, not your application managing sensible data to be protected against a man-in-the-middle.

You can override this behavior, typically for forcing your own KeyStore because your server requires mutual TLS.
You'll have to edit the `ssl` block in [`gatling.conf`](https://github.com/gatling/gatling/blob/main/gatling-core/src/main/resources/gatling-defaults.conf#L53-L64).

Gatling accepts files in jks and p12 formats.

## SSLContext

By default, each virtual user will have its own `SSLContext` and `SSLSession`.
This behavior is realistic when it comes to simulating web traffic so your server has to deal with the proper number of `SSLSession`.

You can only have a shared `SSLContext` if you decide to [shareConnections]({{< ref "protocol#shareconnections" >}}).

## Disabling OpenSSL

By default, Gatling uses [BoringSSL](https://opensource.google.com/projects/boringssl) (Google' fork of OpenSSL) to perform TLS.
This implementation is more efficient than the JDK's one, especially on JDK8.
It's also the only supported solution for HTTP/2 in Gatling with JDK8.

If you want to revert to using JDK's implementation, you can set the `gatling.ssl.useOpenSsl` property to `false` in `gatling.conf`

## Disabling SNI

By default, since JDK7, JDK enables [SNI](http://en.wikipedia.org/wiki/Server_Name_Indication) by default.
This can cause TLS handshake exceptions, such as `handshake alert:  unrecognized_name` when server names are not properly configured on the server side.
Browsers are more loose than JDK regarding this.

If you want to disable SNI, you can set the `gatling.ssl.enableSni` property to `false` in `gatling.conf`.

## TLSv1.3

Gatling supports TLSv1.3 as long as your Java version supports it as well, which means running **at least 1.8.0_262**.
TLSv1.3 is enabled by default.

## Configuring keystore and truststore

Default Gatling TrustStore is very permissive and doesn't validate certificates,
meaning that it works out of the box with self-signed certificates.

You can pass your own keystore and truststore in `gatling.conf`.

[perUserKeyManagerFactory]({{< ref "protocol#peruserkeymanagerfactory" >}}) can be used to set the `KeyManagerFactory` for each virtual user.
