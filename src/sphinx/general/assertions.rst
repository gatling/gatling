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

* ``global``: use statistics calculated from all requests.

* ``forAll``: use statistics calculated for each individual request.

* ``details(path)``: use statistics calculated from a group or a request. The path is defined like a Unix filesystem path.

For example, to perform an assertion on the request ``Index`` in the group ``Search``, use::

  details("Search" / "Index")


.. note::

  When ``path`` is a group, assertions are made against the group's response time, not its cumulated time.
  For more information on the distinction between groups response time and cumulated time, see :ref:`the Groups timings documentation <groups-timings>`.

Statistics
==========

* ``responseTime``: target the response time in milliseconds.

* ``allRequests``: target the number of requests.

* ``failedRequests``: target the number of failed requests.

* ``successfulRequests``: target the number of successful requests.

* ``requestsPerSec``: target the rate of requests per second.

Selecting the metric
====================

Applicable to response time
---------------------------

* ``min``: perform the assertion on the minimum of the metric.

* ``max``: perform the assertion on the maximum of the metric.

* ``mean``: perform the assertion on the mean of the metric.

* ``stdDev``: perform the assertion on the standard deviation of the metric.

* ``percentile1``: perform the assertion on the first percentile of the metric.

* ``percentile2``: perform the assertion on the second percentile of the metric.

Applicable to number of requests (all, failed or successful)
------------------------------------------------------------

* ``percent``: use the value as a percentage between 0 and 100.

* ``count``: perform the assertion directly on the count of requests.

Condition
=========

Conditions can be chained to apply several conditions on the same metric.

* ``lessThan(threshold)``: check that the value of the metric is less than the threshold.

* ``greaterThan(threshold)``: check that the value of the metric is greater than the threshold.

* ``between(thresholdMin, thresholdMax)``: check that the value of the metric is between two thresholds.

* ``is(value)``: check that the value of the metric is equal to the given value.

* ``in(sequence)``: check that the value of metric is in a sequence.

Putting it all together
=======================

To help you understand how to use assertions, here is a list of examples :

::

  // Assert that the max response time of all requests is less than 100 ms
  setUp(...).assertions(global.responseTime.max.lessThan(100))

  // Assert that every requests has no more than 5% of failing requests
  setUp(...).assertions(forAll.failedRequests.percent.lessThan(5))

  // Assert that the percentage of failed requests named "Index" in the group "Search"
  // is exactly 0 %
  setUp(...).assertions(details("Search" / "Index").failedRequests.percent.is(0))

  // Assert that the rate of requests per seconds for the group "Search"
  setUp(...).assertions(details("Search").requestsPerSec.between(100, 1000))
