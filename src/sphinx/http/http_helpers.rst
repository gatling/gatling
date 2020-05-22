:tocdepth: 2

.. _http-helpers:

############
HTTP Helpers
############

.. _http-helpers-cookie:

Dealing with Cookies
====================

Gatling supports cookies out-of-the-box and transparently, just like a browser would.

However, some use cases require a more fine grain control.

.. _http-helpers-cookie-add:

Adding a Cookie
---------------

One might want to manually add or compute a cookie:

.. includecode:: code/HttpHelperSample.scala#cookie

Cookie can also take more optional parameters::

  Cookie(name: Expression[String], value: Expression[String])
    .withDomain(domain: String)
    .withPath(path: String)
    .withMaxAge(maxAge: Int)
    .withSecure(secure: Boolean)

domain is optional, defaulting to base url domain
path is optional, defaulting to "/"
maxAge is optional, defaulting to ``Long.MinValue``
secure is optional, defaulting to false

.. _http-helpers-cookie-get:

Getting a Cookie Value
----------------------

Get the cookie value and put it in the session

.. includecode:: code/HttpHelperSample.scala#getCookie

CookieKey can also take more optional parameters::

  CookieKey(name: Expression[String])
    .withDomain(domain: String)
    .withPath(path: String)
    .withSecure(secure: Boolean)
    .saveAs(key: String)

domain is optional, defaulting to base url domain
path is optional, defaulting to "/"
secure is optional, defaulting to false, means you only want secured cookies
saveAs is optional, defaulting to ``name`` param

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

