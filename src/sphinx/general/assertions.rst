.. _assertions:

##########
Assertions
##########

Concepts
========

The Assertions API is used to verify that global statistics like response time or number of failed requests matches expectations for a whole simulation.

Assertions are registered for a simulation using the method ``assertions`` on the ``setUp``. For example:

.. includecode:: code/AssertionSample.scala#setUp

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

For example, to perform an assertion on the request ``MyRequest``, use:

.. includecode:: code/AssertionSample.scala#details

and to perform an assertion on the request ``MyRequest`` in the group ``MyGroup``, use:

.. includecode:: code/AssertionSample.scala#details-group

For WebSockets it takes the name of the check and not the name of the request. ``ws.checkTextMessage("use this name")``

.. note::

  When ``path`` is a group, assertions are matched against the cumulated response time, not the group total duration.
  For more information on the distinction between groups cumulated response time and duration, see :ref:`the Groups timings documentation <groups-timings>`.

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

* ``percentile1``: perform the assertion on the 1st percentile of the metric, as configured in ``gatling.conf`` (default is 50th).

* ``percentile2``: perform the assertion on the 2nd percentile of the metric, as configured in ``gatling.conf`` (default is 75th).

* ``percentile3``: perform the assertion on the 3rd percentile of the metric, as configured in ``gatling.conf`` (default is 95th).

* ``percentile4``: perform the assertion on the 4th percentile of the metric, as configured in ``gatling.conf`` (default is 99th).

* ``percentile(value: Double)``: perform the assertion on the given percentile of the metric. Parameter is a percentage, between 0 and 100.

Applicable to number of requests (all, failed or successful)
------------------------------------------------------------

* ``percent``: use the value as a percentage between 0 and 100.

* ``count``: perform the assertion directly on the count of requests.

Condition
=========

Conditions can be chained to apply several conditions on the same metric.

* ``lt(threshold)``: check that the value of the metric is less than the threshold.

* ``lte(threshold)``: check that the value of the metric is less than or equal to the threshold.

* ``gt(threshold)``: check that the value of the metric is greater than the threshold.

* ``gte(threshold)``: check that the value of the metric is greater than or equal to the threshold.

* ``between(thresholdMin, thresholdMax)``: check that the value of the metric is between two thresholds.

* ``between(thresholdMin, thresholdMax, inclusive = false)``: same as above but doesn't include bounds

* ``around(value, plusOrMinus)``: check that the value of the metric is around a target value plus or minus a given margin.

* ``around(value, plusOrMinus, inclusive = false)``: same as above but doesn't include bounds

* ``deviatesAround(target, percentDeviationThreshold)``: check that metric is around a target value plus or minus a given relative margin

* ``deviatesAround(target, percentDeviationThreshold, inclusive = false)``: same as above but doesn't include bounds

* ``is(value)``: check that the value of the metric is equal to the given value.

* ``in(sequence)``: check that the value of metric is in a sequence.

Putting it all together
=======================

To help you understand how to use assertions, here is a list of examples :

.. includecode:: code/AssertionSample.scala#examples

Reports
=======

If a simulation defines assertions, Gatling will generate 2 reports in the ``js`` result directory:

* a JSON file
* a JUnit file

The latter can be used for example with Jenkin's JUnit plugin.

Here are some examples:

.. highlight:: json

::

  [
    {
      "path": "Global",
      "target": "max of response time",
      "condition": "is less than",
      "expectedValues": [50],
      "result": false,
      "message": "Global: max of response time is less than 50",
      "actualValue": [145]
    },
    {
      "path": "requestName",
      "target": "percent of successful requests",
      "condition": "is greater than",
      "expectedValues": [95],
      "result": true,
      "message": "requestName: percent of successful requests is greater than 95",
      "actualValue": [100]
    }
  ]

.. highlight:: xml

::

  <testsuite name="GoogleTest" tests="2" errors="0" failures="1" time="0">
    <testcase name="Global: max of response time is less than 50" status="false" time="0">
      <failure type="Global">Actual value: 145</failure>
    </testcase>
    <testcase name="selfSigned: percent of successful requests is greater than 95" status="true" time="0">
      <system-out>selfSigned: percent of successful requests is greater than 95</system-out>
    </testcase>
  </testsuite>
