.. _http-ws:

#########
Websocket
#########

Websocket support is an extension to the HTTP DSL, whose entry point is the ``ws(requestName: Expression[String])`` method.

Websocket protocol is very different from the HTTP one as the communication is 2 ways: both client-to-server and server-to-client, so the model is different from the HTTP request/response pair.

As a consequence, main HTTP branch and a Websocket branch can exist in a Gatling scenario in a dissociated way, in parallel.
When doing so, each flow branch has it's own state, so a user might have to reconciliate them, for example when capturing data from a websocket check and wanting this data to be available to the HTTP branch.

Common operations
=================

.. _http-ws-name:

If you want to deal with several websockets per virtual users, you have to give them a name and pass this name on each ws operation:

``wsName(name: String)``

For example::

  ws("WS Operation").wsName("myCustomName")

Of course, this step is not required if you deal with one single websocket per virtual user.

.. _http-ws-open:

Open
----

The first thing is to open a websocket:

``open(url: Expression[String])``

For example::

  .exec(ws("Connect WS").open("/room/chat?username=steph"))


.. _http-ws-close:

Close
-----

When you're done with a websocket, you can close it:

``close``

For example::

  .exec(ws("Close WS").close)

.. _http-ws-send:

Sending a message
-----------------

One can send 2 forms of messages: binary and text:

``sendText(text: Expression[String])``
``sendBytes(bytes: Expression[Array[Byte]])``

For example::

  .exec(ws("Message")
    .sendText("""{"text": "Hello, I'm ${id} and this is message ${i}!"}"""))

Getting Server Messages
=======================

Dealing with incoming messages from the server is done with checks, passed with the usual ``check()`` method.

Gatling currently only support one check at a time per websocket.

.. _http-ws-check-set:

Setting a Check
---------------

Checks can be set in 2 ways.

First, when sending a message::

  exec(ws("Send").sendText("hello").check(...))


Then, directly from the main HTTP flow::

  exec(ws("Set Check").check(...))

If a check was already registered on the websocket at this time, it's considered as failed and replaced with the new one.

.. _http-ws-check-cancel:

Cancelling a Check
------------------

One can decide to cancel a pending check::

  exec(ws("Cancel Check").cancelCheck)

.. _http-ws-check-build:

Building a Check
----------------

Now, to the matter at heart, how to build a websocket check.

**Step 1: Blocking or non Blocking**

The first thing is to decide if the main HTTP flow is blocked until the check completes or not.

``wsListen`` creates a non blocking check: the main HTTP flow will go on and Gatling will listen for websocket incoming messages on the background.

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

Here are an example::

  exec(ws("Send Message")
         .sendText("hello, I'm Stephane")
         .check(wsListen.within(30 seconds).until(1).regex("hello (.*)").saveAs("name"))


Reconciliating
--------------

One complex thing is that, when using non blocking checks that save data, state is stored in a different flow than the main one.

So, one has to reconciliate the main flow state and the websocket flow one.

This can be done:

* implicitly when performing an action on the websocket from the main flow, such as send a message to the server
* explicitly with the ``reconciliate`` method.

::

  exec(ws("Reconciliate states").reconciliate)









