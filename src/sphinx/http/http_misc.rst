:tocdepth: 2

.. _http-misc:

#################
HTTP Misc Helpers
#################

.. _http-misc-cookie:

Dealing with Cookies
====================

Cookie support is enabled by default and then Gatling handles Cookies transparently, just like a browser would.

However, some use cases require a more fine grain control.

.. _http-misc-cookie-add:

Adding a Cookie
---------------

One might want to manually add or compute a cookie::

  .exec(addCookie(Cookie("name", "value")))

Cookie can also take more optional parameters::

  Cookie(name: Expression[String], value: Expression[String])
    .withDomain(domain: Expression[String])
    .withPath(path: Expression[String])
    .withExpires(expires: Long)
    .withMaxAge(maxAge: Int)

.. _http-misc-cookie-flush-session:

Flushing Session Cookies
------------------------

One might want to simulate closing a browser, so Session cookies are dropped but permanent cookies are still there::

  .exec(flushSessionCookies)

.. _http-misc-cookie-flush-all:

Flushing All Cookies
--------------------

One might want to flush the whole CookieJar::

  .exec(flushCookieJar)

.. _http-misc-cache:

Dealing with Caching
====================

.. _http-misc-cache-flush:

Flushing the Cache
------------------

One might want to flush the whole HTTP cache (for the virtual user) ::

  .exec(flushHttpCache)

