######################################
Implementing your own Protocol Support
######################################

.. note:: Work in Progress! See also tutorial by James Gregory: https://github.com/jagregory/gatling/blob/master/GatlingProtocolBreakdown.md

The Action
==========

An Action is the actual component in charge of "doing something", like sending a request (or looping, routing, etc).
It must:

* extend the ``com.excilys.ebi.gatling.core.action.Action`` trait
* implement the ``def execute(session: Session)`` method.

This method has to perform the following operations:

* send the request
* retrieve the response
* perform checks on the response (optional)
* save elements from the checks into the session for further usage (optional)
* log the outcome::

	DataWriter.logRequest(session.scenarioName,
	                      session.userId,
	                      "Request " + requestName,
	                      requestStartDate: Long,
	                      responseEndDate: Long,
	                      endOfRequestSendingDate: Long,
	                      endOfRequestSendingDate: Long,
	                      requestResult: RequestStatus,
	                      requestMessage: String)

* calling the next action in the chain::

	next ! session

Note that an Action is basically an Akka Actor that handles Session messages. An Action instance is to be shared among users.

The ActionBuilder
=================

An ActionBuilder is what is expected in the DSL. It is used at engine start up for building Action instances.

An ActionBuilder is to be stateless and immutable so it can be reused at multiple places in the scenario and produce multiple unrelated Actions instances.

It expects an implementation of the following methods::

	private[gatling] def withNext(next: ActorRef): ActionBuilder

returns a new ActionBuilder instance with a reference to the given ActorRef. An ``ActorRef`` is a stub on an Akka actor.
This method is called at engine startup when attaching the builder in the scenario chain.

::

	private[gatling] def build(registry: ProtocolConfigurationRegistry): ActorRef


builds the Action and starts it. It take a ProtocolConfigurationRegistry parameter that is basically a Map of protocol specific option (such as outgoing proxy for HTTP).
For starting and returning an Actor from an Action instance, use the following::

	com.excilys.ebi.gatling.core.action.system.actorOf(Props(myActionInstance))
