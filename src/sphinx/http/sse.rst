.. _http-sse:

#######################
SSE (Server Sent Event)
#######################

SSE support is an extension to the HTTP DSL, whose entry point is the ``sse(requestName: Expression[String])`` method.

Common operations
=================

.. _http-sse-name:

If you want to deal with several SSE streams per virtual users, you have to give them a name and pass this name on each SSE operation:

``sseName(name: String)``

For example:

.. includecode:: code/Sse.scala#sseName

Of course, this step is not required if you deal with one single SSE stream per virtual user.

.. _http-sse-open:

Open
----

The first thing is to get a server sent event:

``open(url: Expression[String])``

For example:

.. includecode:: code/Sse.scala#sseOpen


.. note:: Gatling automatically sets ``Accept`` header to ``text/event-stream`` and ``Cache-Control`` to ``no-cache``.

.. _http-sse-close:

Close
-----

When you're done with a SSE stream, you can close it:

``close``

For example:

.. includecode:: code/Sse.scala#sseClose

Server Messages: Checks
=======================

Dealing with incoming messages from the server is done with checks, passed with the usual ``check()`` method.

Gatling currently only support one check at a time per SSE stream.

.. _http-sse-check-set:

Set a Check
-----------

Checks can be set in 2 ways.

First, when sending a message:

.. includecode:: code/Sse.scala#check-from-message

Then, directly from the main HTTP flow:

.. includecode:: code/Sse.scala#check-from-flow

If a check was already registered on the server sent event at this time, it's considered as failed and replaced with the new one.

.. _http-sse-check-cancel:

Cancel a Check
--------------

One can decide to cancel a pending check:

.. includecode:: code/Sse.scala#cancel-check

.. _http-sse-check-build:

Build a Check
-------------

Now, to the matter at heart, how to build a server sent event check.

SSE support uses the same checks as WebSockets.
So, please refer to the WebSocket section :ref:`Build a Check <http-ws-check-build>` for more details.

Here are few examples:

.. includecode:: code/Sse.scala#build-check

.. _http-sse-check-reconciliate:

Reconciliate
------------

One complex thing is that, when using non blocking checks that save data, state is stored in a different flow than the main one.

So, one has to reconciliate the main flow state and the WebSocket flow one.

This can be done:

* implicitly when performing an action on the WebSocket from the main flow, such as send a message to the server
* explicitly with the ``reconciliate`` method.

.. includecode:: code/Sse.scala#reconciliate

.. _http-sse-check-conf:

Configuration
=============

Server sent event support uses the same parameter as the HttpProtocol:

``baseURL(url: String)``: serves as root that will be prepended to all relative server sent event urls

``baseURLs(urls: String*)``: serves as round-robin roots that will be prepended to all relative server sent event urls

Example
=======

Here's an example that runs against a stock market sample:

.. includecode:: code/Sse.scala#stock-market-sample
