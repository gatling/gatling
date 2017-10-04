:tocdepth: 2

.. _http-helpers:

############
HTTP Helpers
############

.. _http-helpers-cookie:

Dealing with Cookies
====================

Cookie support is enabled by default and then Gatling handles Cookies transparently, just like a browser would.

However, some use cases require a more fine grain control.

.. _http-helpers-cookie-add:

Adding a Cookie
---------------

One might want to manually add or compute a cookie:

.. includecode:: code/HttpHelperSample.scala#cookie

Cookie can also take more optional parameters::

  Cookie(name: Expression[String], value: Expression[String])
    .withDomain(domain: Expression[String])
    .withPath(path: Expression[String])
    .withMaxAge(maxAge: Int)

domain is optional, defaulting to base url domain
path is optional, defaulting to "/"
maxAge is optional, defaulting to ``Long.MinValue``

.. _http-helpers-cookie-flush-session:

Flushing Session Cookies
------------------------

One might want to simulate closing a browser, so Session cookies are dropped but permanent cookies are still there:

.. includecode:: code/HttpHelperSample.scala#flushSessionCookies

.. _http-helpers-cookie-flush-all:

Flushing All Cookies
--------------------

One might want to flush the whole CookieJar:

.. includecode:: code/HttpHelperSample.scala#flushCookieJar

.. _http-helpers-cache:

Dealing with Caching
====================

.. _http-helpers-cache-flush:

Flushing the Cache
------------------

One might want to flush the whole HTTP cache (for the virtual user) :

.. includecode:: code/HttpHelperSample.scala#flushHttpCache

