.. _http-polling:

############
HTTP Polling
############

HTTP polling is an extension to the HTTP DSL, whose entry point is the ``polling`` method.

Common operations
=================

.. _http-polling-name:

If you want to deal with several pollers with per virtual users,
you have to give them a name and pass this name on each polling operation:

``pollerName(name:String)``

For example:

.. includecode:: code/Polling.scala#pollerName

Of course, this step is not required if you deal with one single poller per virtual user.

Start polling
-------------

The first thing to do is start the polling, by specifying the request and how often it will run:

``every(period).exec(request)``

For example:

.. includecode:: code/Polling.scala#pollerStart

Stop polling
------------

When you don't need to poll a request anymore, you can stop the poller:

``poller.stop``

For example:

.. includecode:: code/Polling.scala#pollerStop

.. note::
  Stopping a poller works works in the same fashion as SSE or WebSockets ``reconciliate``:
  When stopping a poller, the poller flow state (e.g. the session) is merged with the main flow state.