.. _http-ws:

#########
WebSocket
#########

Functional specs support was initially contributed by `Andrew Duffy <https://github.com/amjjd>`_.

WebSocket support is an extension to the HTTP DSL, whose entry point is the ``ws(requestName: Expression[String])`` method.

WebSocket protocol is very different from the HTTP one as the communication is 2 ways: both client-to-server and server-to-client, so the model is different from the HTTP request/response pair.

As a consequence, main HTTP branch and a WebSocket branch can exist in a Gatling scenario in a dissociated way, in parallel.
When doing so, each flow branch has it's own state, so a user might have to reconciliate them, for example when capturing data from a websocket check and wanting this data to be available to the HTTP branch.

Common operations
=================

.. _http-ws-name:

If you want to deal with several WebSockets per virtual users, you have to give them a name and pass this name on each ws operation:

``wsName(name: String)``

For example:

.. includecode:: code/WebSocket.scala#wsName

Of course, this step is not required if you deal with one single WebSocket per virtual user.

.. _http-ws-open:

Open
----

The first thing is to open a WebSocket:

``open(url: Expression[String])``

For example:

.. includecode:: code/WebSocket.scala#wsOpen

.. _http-ws-close:

Close
-----

When you're done with a WebSocket, you can close it:

``close``

For example:

.. includecode:: code/WebSocket.scala#wsClose

.. _http-ws-send:

Send a Message
--------------

One can send 2 forms of messages: binary and text:

* ``sendText(text: Expression[String])``
* ``sendBytes(bytes: Expression[Array[Byte]])``

For example:

.. includecode:: code/WebSocket.scala#sendText

Server Messages: Checks
=======================

Dealing with incoming messages from the server is done with checks, passed with the usual ``check()`` method.

Gatling currently only support one check at a time per WebSocket.

.. _http-ws-check-set:

Set a Check
-----------

Checks can be set in 2 ways.

First, when sending a message:

.. includecode:: code/WebSocket.scala#check-from-message

Then, directly from the main HTTP flow:

.. includecode:: code/WebSocket.scala#check-from-flow

If a check was already registered on the WebSocket at this time, it's considered as failed and replaced with the new one.

.. _http-ws-check-cancel:

Cancel a Check
--------------

One can decide to cancel a pending check:

.. includecode:: code/WebSocket.scala#cancel-check

.. _http-ws-check-build:

Build a Check
-------------

Now, to the matter at heart, how to build a WebSocket check.

**Step 1: Blocking or non Blocking**

The first thing is to decide if the main HTTP flow is blocked until the check completes or not.

``wsListen`` creates a non blocking check: the main HTTP flow will go on and Gatling will listen for WebSocket incoming messages on the background.

``wsAwait`` creates a blocking check: the main HTTP flow is blocked until the check completes.

**Step 2: Set the Timeout**

``within(timeout: FiniteDuration)``

**Step 3: Exit condition**

``until(count: Int)``: the check will succeed as soon as Gatling has received the expected count of matching messages

``expect(count: Int)``: Gatling will wait until the timeout and the check will succeed if it has received the expected count of matching messages

``expect(range: Range)``: same as above, but use a range instead of a single expected count

**Step 4: Matching condition**

Websocket checks support the same kind of operations as for HTTP bodies:

``regex(expression: Expression[String])``: use a regular expression

``jsonPath(path: Expression[String])``: use JsonPath

``jsonpJsonPath(path: Expression[String])``: use JsonPath on a JSONP String

See :ref:`HTTP counterparts <http-check>` for more details.

**Step 5: Saving** (optional)

Just like regular HTTP checks, one can use checks for saving data into the virtual user's session.

Here are an example:

.. includecode:: code/WebSocket.scala#check-example

.. _http-ws-check-reconciliate:

Reconciliate
------------

One complex thing is that, when using non blocking checks that save data, state is stored in a different flow than the main one.

So, one has to reconciliate the main flow state and the WebSocket flow one.

This can be done:

* implicitly when performing an action on the WebSocket from the main flow, such as send a message to the server
* explicitly with the ``reconciliate`` method.

.. includecode:: code/WebSocket.scala#reconciliate

.. _http-ws-check-conf:

Configuration
=============

Websocket support introduces new parameters on HttpProtocol:

``wsBaseURL(url: String)``: similar to standard ``baseURL`` for HTTP, serves as root that will be prepended to all relative WebSocket urls

``wsBaseURLs(urls: String*)``: similar to standard ``baseURLs`` for HTTP, serves as round-robin roots that will be prepended to all relative WebSocket urls

``wsReconnect``: automatically reconnect a WebSocket that would have been closed by someone else than the client.

``wsMaxReconnects(max: Int)``: set a limit on the number of times a WebSocket will be automatically reconnected

Example
=======

Here's an example that runs against `Play 2.2 <https://www.playframework.com/download#older-versions>`_'s chatroom sample (beware that this sample is missing from Play 2.3 and above):

.. includecode:: code/WebSocket.scala#chatroom-example

