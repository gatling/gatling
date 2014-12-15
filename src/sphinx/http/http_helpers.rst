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

.. includecode:: code/HttpHelpers.scala#cookie

Cookie can also take more optional parameters::

  Cookie(name: Expression[String], value: Expression[String])
    .withDomain(domain: Expression[String])
    .withPath(path: Expression[String])
    .withExpires(expires: Long)
    .withMaxAge(maxAge: Int)

.. _http-helpers-cookie-flush-session:

Flushing Session Cookies
------------------------

One might want to simulate closing a browser, so Session cookies are dropped but permanent cookies are still there:

.. includecode:: code/HttpHelpers.scala#flushSessionCookies

.. _http-helpers-cookie-flush-all:

Flushing All Cookies
--------------------

One might want to flush the whole CookieJar:

.. includecode:: code/HttpHelpers.scala#flushCookieJar

.. _http-helpers-cache:

Dealing with Caching
====================

.. _http-helpers-cache-flush:

Flushing the Cache
------------------

One might want to flush the whole HTTP cache (for the virtual user) :

.. includecode:: code/HttpHelpers.scala#flushHttpCache

