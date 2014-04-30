.. _assertions:

##########
Assertions
##########

Concepts
========

The Assertions API is used to verify that global statistics like response time or number of failed requests matches expectations for a whole simulation.

Assertions are registered for a simulation using the method ``assertions`` on the ``setUp``. For example::

	setUp(...).assertions(
		global.responseTime.max.lessThan(50),
		global.successfulRequests.percent.greaterThan(95)
	)

This method takes as many assertions as you like.

The API provides a dedicated DSL for chaining the following steps :

1. defining the scope of the assertion
2. selecting the statistic
3. selecting the metric
4. defining the condition

All the assertions are evaluated after running the simulation. If at least one assertion fails, the simulation fails.

Scope
=====

An assertion can test a statistic calculated from all requests or only a part.

``global``: use statistics calculated from all requests.

``details(path)``: use statistics calculated from a group or a request. The path is defined like a Unix filesystem path.
For example, to perform an assertions on the request ``Index`` in the group ``Search``, use::

	details("Search" / "Index")

Statistics
==========

``responseTime``: target the reponse time in milliseconds.

``allRequests``: target the number of requests.

``failedRequests``: target the number of failed requests.

``successfulRequests``: target the number of successful requests.

``requestsPerSec``: target the rate of requests per second.

Selecting the metric
====================

Applicable to response time
---------------------------

``min``: perform the assertion on the minimum of the statistic.

``max``: perform the assertion on the maximum of the statistic.

``mean``: perform the assertion on the mean of the statistic.

``stdDev``: perform the assertion on the standard deviation of the statistic.

``percentile1``: perform the assertion on the first percentile of the statistic.

``percentile2``: perform the assertion on the second percentile of the statistic.

Applicable to number of requests (all, failed or successful)
------------------------------------------------------------

``percent``: use the value as a percentage between 0 and 100.

``count``: perform the assertion directly on the count of requests.

Condition
=========

Conditions can be chained to apply several conditions on the same statistic.

``lessThan(threshold)``: check that the value of the statistic is less than the threshold.

``greaterThan(threshold)``: check that the value of the statistic is greater than the threshold.

``between(thresholdMin, thresholdMax)``: check that the value of the statistic is between two thresholds.

``is(value)``: check that the value of the statistic is equal to the given value.

``in(sequence)``: check that the value of statistic is in a sequence.

``assert(condition, message)``: create a custom condition on the value of the statistic.

The first argument is a function that take an Int (the value of the statistics) and return a Boolean which is the result of the assertion.

The second argument is a function that take a String (the name of the statistic) and a Boolean (result of the assertion) and return a message that describes the assertion as a String.

For example::

	assert(
		value => value % 2 == 0,
		(name, result) => name + " is even : " + result)

This will assert that the value is even.

Putting it all together
=======================

To help you understand how to use assertions, here is a list of examples :

::

  // Assert that the max response time of all requests is less than 100 ms
  setUp(...).assertions(global.responseTime.max.lessThan(100))

  // Assert that the percentage of failed requests named "Index" in the group "Search"
  // is exactly 0 %
  setUp(...).assertions(details("Search" / "Index").failedRequests.percent.is(0))

  // Assert that the rate of requests per seconds for the group "Search"
  // is between 100 and 1000
  setUp(...).assertions(details("Search").requestsPerSec.greaterThan(100).lessThan(1000))

  // Same as above but using between
  setUp(...).assertions(details("Search").requestsPerSec.between(100, 1000))
